package gsrs.stagingarea.repository;

import gsrs.stagingarea.model.ImportData;
import gsrs.repository.GsrsVersionedRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public interface ImportDataRepository extends GsrsVersionedRepository<ImportData, UUID> {

    @Query("select d.data from ImportData d where d.recordId = ?1 and d.version = ?2")
    public String retrieveByIDAndVersion(UUID id, int version);

    @Query("select d.data from ImportData d where d.instanceId = ?1")
    public String retrieveByInstanceID(UUID id);

    @Query("select d.instanceId from ImportData d where d.recordId = ?1")
    public List<UUID> findInstancesForRecord(UUID id);

    @Query("select d from ImportData d where d.recordId = ?1")
    public List<ImportData> retrieveDataForRecord(UUID id);

    @Modifying
    @Transactional
    @Query("delete from ImportData i where i.recordId = ?1")
    void deleteByRecordId(UUID recordId);

    @Modifying
    @Transactional
    @Query("delete from ImportData i where i.recordId = ?1 and i.version = ?2")
    void deleteByRecordIdAndVersion(UUID recordId, int version);

    @Modifying
    @Transactional
    @Query("update ImportData i set data= ?3 where i.recordId = ?1 and i.version = ?2")
    void updateDataByRecordIdAndVersion(UUID recordId, int version, String data);
}
