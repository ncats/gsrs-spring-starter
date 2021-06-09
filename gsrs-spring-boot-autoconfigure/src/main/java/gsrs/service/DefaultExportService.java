package gsrs.service;

import gov.nih.ncats.common.io.IOUtil;
import gsrs.autoconfigure.GsrsExportConfiguration;
import ix.core.controllers.EntityFactory;
import ix.ginas.exporters.ExportDir;
import ix.ginas.exporters.ExportMetaData;
import ix.ginas.exporters.ExportProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DefaultExportService implements ExportService{

    private GsrsExportConfiguration config;

    private File rootDir;

    private ConcurrentHashMap<String,ExportMetaData> inProgress = new ConcurrentHashMap<>();

    //in GSRS 2.x we never cleared our inProgress map !!
    //maybe use spring schedule
    @Scheduled(fixedDelay = 1000* 60 * 60 ) // every hour?
    public void clearProgressCache(){
        //assuming the processor runs and updates the meta data,
        //remove anything that is done as it should be in the .metadata file and we can
        //re-read it in a stale state later
        Iterator<Map.Entry<String, ExportMetaData>> iter = inProgress.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry<String, ExportMetaData> entry = iter.next();
            if(entry.getValue().cancelled || entry.getValue().isComplete()){
                iter.remove();
            }
        }
    }
    @Autowired
    public DefaultExportService(GsrsExportConfiguration config){
        this.config = config;
        rootDir = config.getPath();
    }
    @Override
    public Optional<ExportDir.ExportFile<ExportMetaData>> getFile(String username, String filename) throws IOException {
            return new ExportDir<>(new File(rootDir, username), ExportMetaData.class).getFile(filename);
//        System.out.println("trying to download " + downloadFile);
//            return downloadFile.orElseThrow(()->new FileNotFoundException("could not find file for user "+ username + ":" +  filename))
//                    .getInputStreamOutputStream();
//        File[] files = getFiles(getExportDirFor(username), fname);
//        File downloadFile = files[0];
//        if(downloadFile.exists()){
//            return new BufferedInputStream(new FileInputStream(downloadFile));
//        }
//        throw new FileNotFoundException("could not find file for user "+ username + ":" +  fname);
        }

    private static File getExportMetaDirFor(File parentDir){
        File metaDirectory = new File(parentDir, "meta");
        try {
            IOUtil.mkdirs(metaDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("error getting or creating export meta directory for " + parentDir.getName(), e);
        }
        return metaDirectory;
    }

    //TODO: probably change the way this is stored to make it a little easier
    //the use of metadata files, right now, is probably overkill
    //If we must have 1 metafile per, I'd like to have a separate folder
    //perhaps?
    @Override
    public List<ExportMetaData> getExplicitExportMetaData(String username){
        EntityFactory.EntityMapper em = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER();
        File metaDirectory = getExportMetaDirFor(new File(rootDir, username));

        File[] files = metaDirectory.listFiles();
        //null if directory doesn't exist or there's an IO problem
        if(files ==null){
            return Collections.emptyList();
        }
        return Arrays.stream(files)

                .filter(f->f.getName().endsWith(".metadata"))
//                     .peek(f-> System.out.println(f.getAbsolutePath()))
                .map(f->{
                    try {
                        return em.readValue(f, ExportMetaData.class);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(m->{
                    ExportMetaData em2=inProgress.get(m.id);
                    if(em2==null){
                        return m;
                    }else{
                        return em2;
                    }
                })
                .filter(m-> m.started !=null) // this shouldn't happen anymore but just incase...
                //newest first
                // GSRS-920 fixed sorting which used to break on int overflow when users had lots of old and new files
                .sorted(Comparator.comparing(ExportMetaData::getStarted).reversed())
                .collect(Collectors.toList());
    }

    private <T> ExportProcess createExportProcessFor(ExportMetaData metadata, Supplier<Stream<T>> substanceSupplier){

        //might be a better way to do this as a one-liner using paths
        //but I don't think Path's path can contain null
        String username = metadata.username;
        ExportDir.ExportFile<ExportMetaData> exportFile =  createExportFileFor(metadata, username);


        inProgress.put(metadata.id, metadata);
        return new ExportProcess<T>(exportFile, substanceSupplier);
    }

    private ExportDir.ExportFile<ExportMetaData> createExportFileFor(ExportMetaData metadata, String username){
        File exportDir = new File(rootDir,username);
        try {
            return new ExportDir<>(exportDir, ExportMetaData.class).createFile(metadata.getFilename(), metadata);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    @Override
    public <T> ExportProcess createExport(ExportMetaData metaData, Supplier<Stream<T>> entitySupplier) throws Exception{
        return createExportProcessFor(metaData, entitySupplier);
    }
    @Override
    public Optional<ExportMetaData> getStatusFor(String username, String downloadID) {
        ExportMetaData emeta=inProgress.computeIfAbsent(downloadID, (k)->{
            return getExplicitExportMetaData(username)
                    .stream()
                    .filter(em->em.id.equals(downloadID))
                    .findFirst()
                    .orElse(null);
        });
        return Optional.ofNullable(emeta);
    }

    @Override
    public void remove(ExportMetaData meta){
        inProgress.remove(meta.id);

        Optional<ExportDir.ExportFile<ExportMetaData>> downloadFile = null;
        try {
            downloadFile = new ExportDir<>(new File(rootDir, meta.username), ExportMetaData.class).getFile(meta.getFilename());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(downloadFile !=null && downloadFile.isPresent()){
            downloadFile.get().delete();
        }


    }


}
