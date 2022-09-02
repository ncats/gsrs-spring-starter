package gsrs.holdingarea.repository;

import gsrs.holdingarea.model.ImportData;
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

    @Query("select d.instanceId from ImportData d where d.recordId = ?1")
    public List<UUID> findInstancesForRecord(UUID id);

    @Modifying
    @Transactional
    @Query("delete from ImportData i where i.recordId = ?1")
    void deleteByRecordId(UUID recordId);

}
