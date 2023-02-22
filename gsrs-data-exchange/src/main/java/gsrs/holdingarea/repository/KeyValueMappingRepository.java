package gsrs.holdingarea.repository;

import gsrs.holdingarea.model.KeyValueMapping;
import gsrs.repository.GsrsVersionedRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface KeyValueMappingRepository extends GsrsVersionedRepository<KeyValueMapping, UUID> {

    @Query("select k from KeyValueMapping k where k.instanceId = ?1")
    List<KeyValueMapping> retrieveKeyValueMappingsByRecordId(UUID instanceId);

    @Query("select i from KeyValueMapping i where i.key = ?1 and i.value = ?2")
    List<KeyValueMapping> findMappingsByKeyAndValue(String key, String value);

    @Query("select i from KeyValueMapping i where i.key = ?1 and i.value = ?2 and i.recordId != ?3")
    List<KeyValueMapping> findMappingsByKeyAndValueExcludingRecord(String key, String value, UUID excludedRecordId);

    @Query("select k from KeyValueMapping k where k.recordId = ?1")
    List<KeyValueMapping> findRecordsByRecordId(UUID recordId);

    @Modifying
    @Transactional
    @Query("delete from KeyValueMapping i where i.dataLocation = ?1")
    void deleteByDataLocation(String dataLocation);

    @Modifying
    @Transactional
    @Query("delete from KeyValueMapping i where i.recordId = ?1")
    void deleteByRecordId(UUID recordId);

}
