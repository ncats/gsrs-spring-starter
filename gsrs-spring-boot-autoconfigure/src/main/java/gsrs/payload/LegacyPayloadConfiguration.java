package gsrs.payload;

import gov.nih.ncats.common.sneak.Sneak;
import ix.core.models.Payload;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@ConfigurationProperties("ix.core.files.persist")
@Data
public class LegacyPayloadConfiguration {

    public static final String PERSIST_LOCATION_DB = "<DB>";
    public static final String PERSIST_LOCATION_FILE = "<NULL>";
    /*
    # Area to store uploaded files
ix.core.files.persist.location="<DB>"
ix.core.files.persist.maxsize="30MB"
     */
    private String location = PERSIST_LOCATION_DB;
    private DataSize maxsize = DataSize.ofMegabytes(30);

    private File base;

    public File createNewSaveFileFor(Payload payload) {
        return new File (base, payload.id.toString());
    }

    public boolean shouldPersistInDb(){
        return PERSIST_LOCATION_DB.equals(location);
    }

    public void setBase(File base){
        this.base = base;
        if(base !=null){
            try {
                Files.createDirectories(base.toPath());
            } catch (IOException e) {
                Sneak.sneakyThrow(e);
            }
        }
    }

    public Optional<File> getExistingFileFor(Payload payload){
        File temp = new File(base,payload.id.toString());
        if(temp.exists()){
            return Optional.of(temp);
        }
        if(!PERSIST_LOCATION_FILE.equals(location) && ! PERSIST_LOCATION_DB.equals(location) ) {

            File newLoc = new File(location, payload.id.toString());
            if (newLoc.exists()) {
                return Optional.of(newLoc);
            }
        }
        return Optional.empty();
    }
    public Optional<File> createNewPersistFileFor(Payload payload) throws IOException{
        if(PERSIST_LOCATION_FILE.equals(location) ||PERSIST_LOCATION_DB.equals(location) ){
            return Optional.empty();
        }
        File newLoc = new File(location,payload.id.toString());
        File parent = new File(location);
        if(location !=null && !parent.exists()){
            Files.createDirectories(parent.toPath());
        }
        return Optional.of(newLoc);

    }

//    @Bean
//    @ConditionalOnMissingBean
//    public PayloadService legacyPayloadService(PayloadRepository payloadRepository,
//                                                      LegacyPayloadConfiguration configuration, FileDataRepository fileDataRepository) throws IOException {
//
//        return new LegacyPayloadService(payloadRepository, configuration, fileDataRepository);
//    }
}
