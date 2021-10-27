package gsrs.repository;

import ix.core.models.BackupEntity;
import ix.core.models.ETag;
import ix.core.util.EntityUtils.Key;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface BackupRepository extends GsrsRepository<BackupEntity, Long> {

    Optional<BackupEntity> findByRefid(String refid);
    @Query("select e from BackupEntity e")
    Stream<BackupEntity> streamAll();
    
    //TODO: This should be worked out to respect the kind more if needed
    // but that's not always trivial due to subclasses of kinds
    default Optional<BackupEntity> getByEntityKey(Key k){
        Object nid=k.getIdNative();
        if(nid instanceof UUID ||
                nid instanceof String){
            return findByRefid(k.getIdString()); //TODO: this part is inconsistent
            //because the UUIDs considered unique
            //globally, but other IDs are not 
            //considered globally unique
        }else if(nid instanceof Long || nid instanceof Integer ){
            return findByRefid(k.getKind() + ":" + k.getIdString()); 
        }
        return Optional.empty();
    }
   
}
