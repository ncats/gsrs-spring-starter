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
import java.util.UUID;

/**
 * Configuration for {@link LegacyPayloadService}
 * using the GSRS 2.x key-value property files.
 */
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

    /**
     * Get the file associated with the given payload.
     * @param payload the payload to look for.
     * @return an Optional wrapped File if the file is found;
     * {@link Optional#empty()} if not found.
     */
    public Optional<File> getExistingFileFor(Payload payload){
        return getExistingFileFor(payload.id);
    }
    /**
     * Get the file associated with the given payload UUID.
     * @param payloadId the UUID of the payload to look for.
     * @return an Optional wrapped File if the file is found;
     * {@link Optional#empty()} if not found.
     */
    public Optional<File> getExistingFileFor(UUID payloadId){
        String uuidAsString = payloadId.toString();
        File temp = new File(base,uuidAsString);
        if(temp.exists()){
            return Optional.of(temp);
        }
        if(!PERSIST_LOCATION_FILE.equals(location) && ! PERSIST_LOCATION_DB.equals(location) ) {

            File newLoc = new File(location, uuidAsString);
            if (newLoc.exists()) {
                return Optional.of(newLoc);
            }
        }
        return Optional.empty();
    }

    /**
     * Create a new PERSIST File for the given Payload,
     * the returned File will not contain any data and might not even
     * be created yet but you should use this File to write the data for this payload.
     * the Persist File is a more permanent file location
     * @param payload the payload to associate the file to; can not be null and should have a non-null id.
     * @return an Optional wrapped File of the file associated; or {@link Optional#empty()}
     * if a file location could not be used.
     * @throws IOException if there is a problem creating the directory structure for this file.
     */
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
}
