package gsrs.holdingArea.repository;

import gsrs.holdingArea.model.KeyValueMapping;
import gsrs.repository.GsrsVersionedRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KeyValueMappingRepository extends GsrsVersionedRepository<KeyValueMapping, UUID> {

    @Query("select k from KeyValueMapping k where k.recordId = ?1")
    List<KeyValueMapping> retrieveKeyValueMappingsByRecordId(UUID recordId);

    @Query("select i from KeyValueMapping i where i.key = ?1 and i.value = ?2")
    List<KeyValueMapping> findMappingsByKeyAndValue(String key, String value);

}
