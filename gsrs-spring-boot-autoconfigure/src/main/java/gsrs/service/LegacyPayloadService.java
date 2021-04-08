package gsrs.service;

import gov.nih.ncats.common.io.IOUtil;
import gsrs.payload.LegacyPayloadConfiguration;
import gsrs.repository.FileDataRepository;
import gsrs.repository.PayloadRepository;
import ix.core.models.FileData;
import ix.core.models.Payload;
import ix.utils.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
public class LegacyPayloadService implements PayloadService{

    private final PayloadRepository payloadRepository;
    private final FileDataRepository fileDataRepository;
    private final LegacyPayloadConfiguration configuration;
    @Autowired
    public LegacyPayloadService(PayloadRepository payloadRepository,
                                LegacyPayloadConfiguration configuration, FileDataRepository fileDataRepository) throws IOException {
        this.payloadRepository = payloadRepository;
        this.configuration = configuration;
        this.fileDataRepository = fileDataRepository;
        if(configuration.getRootDir() !=null){
            Files.createDirectories(configuration.getRootDir().toPath());
        }
    }

    @Override
    @Transactional
    public Payload createPayload(String name, String mime, InputStream content, PayloadPersistType persistType) throws IOException {
        //Take from GSRS 2.7 PayloadFactory (with minor adjustments)
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        }catch(NoSuchAlgorithmException e){
            throw new IOException(e);
        }
        File tmp = File.createTempFile("___", ".tmp", configuration.getRootDir());

        long size=0L;
        try(OutputStream fos = new BufferedOutputStream(new FileOutputStream (tmp));
            InputStream in = new DigestInputStream(new BufferedInputStream(content), md)) {
            //default buffersize of BufferedOutputStream is 8K
            byte[] buf = new byte[8192];

            size = 0L;
            int bytesRead=0;
            while ( (bytesRead = in.read(buf)) > 0){
                fos.write(buf, 0, bytesRead);
                size += bytesRead;
            }
        }
        Payload payload = null;
        String sha1 = Util.toHex(md.digest());
        Optional<File> preExistingFile = Optional.empty();
        Optional<Payload> preExistingPayload = payloadRepository.findBySha1(sha1);
        if(preExistingPayload.isPresent()){
            payload = preExistingPayload.get();
            preExistingFile = getPayloadAsFile(payload);

        }

        if(payload ==null){
            payload = new Payload();
            payload.sha1= sha1;
            payload.size = size;
            payload.name = name;
            payload.mimeType = mime;
            payload = payloadRepository.saveAndFlush(payload);
        }
        if(!preExistingFile.isPresent()){
           persistFile(tmp, payload, persistType);
        }
        return payload;
    }
    private File persistFile(File tmpFile, Payload payload, PayloadPersistType ptype) throws IOException {
        //file system persist
        //this was copied from GSRS 2.7 PayloadPlugin with minor adjustments

        //TODO does this belong here or in the configuration? maybe here? and make the configuration object a thin model?
        File saveFile = configuration.createNewSaveFileFor(payload);
        Files.move(tmpFile.toPath(), saveFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
        tmpFile.renameTo(saveFile);

        //database persist
        if(ptype==PayloadPersistType.PERM){
            if(configuration.shouldPersistInDb()){
                Optional<FileData> found = fileDataRepository.findBySha1(payload.sha1);
                if (!found.isPresent()){
                    try {
                        FileData fd = new FileData();
//                        fd.data=inputStreamToByteArray(getPayloadAsStream(payload));
                        //katzelda: we have the file already just use it
                        fd.data= Files.readAllBytes(saveFile.toPath());
                        fd.mimeType=payload.mimeType;
                        fd.sha1=payload.sha1;
                        fd.size=payload.size;
                        fileDataRepository.saveAndFlush(fd);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                Optional<File> newLoc = configuration.createNewPersistFileFor(payload);
                if(newLoc.isPresent()){
                    try {
                        Files.copy(saveFile.toPath(),newLoc.get().toPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
        return saveFile;
    }

    @Override
    public Optional<InputStream> getPayloadAsInputStream(Payload payload) throws IOException {
        //this is almost the same as getAsFile except we do some different handling of db fetching to inputstream
        Optional<File> existingFile = configuration.getExistingFileFor(payload);
        if(existingFile.isPresent()){
            return Optional.of(new FileInputStream(existingFile.get()));
        }
        if(configuration.shouldPersistInDb()){
            //check db
            Optional<FileData> data = fileDataRepository.findBySha1(payload.sha1);
            if(data.isPresent()){
                return Optional.of(new ByteArrayInputStream(data.get().data));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<File> getPayloadAsFile(Payload payload) throws IOException{
        Optional<File> existingFile = configuration.getExistingFileFor(payload);
        if(existingFile.isPresent()){
            return existingFile;
        }
        if(configuration.shouldPersistInDb()){
            //check db
            Optional<FileData> data = fileDataRepository.findBySha1(payload.sha1);
            if(data.isPresent()){
                    //save to temp file?
                File tmp = File.createTempFile("___", ".tmp", configuration.getRootDir());
                try(InputStream in = new BufferedInputStream(new ByteArrayInputStream(data.get().data));
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))
                ){
                    IOUtil.copy(in, out);
                }
                return Optional.of(tmp);
            }
        }
        return Optional.empty();
    }
}
