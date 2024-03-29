package gsrs.stagingarea.repository;

import gsrs.stagingarea.model.ImportMetadata;
import gsrs.repository.GsrsVersionedRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImportMetadataRepository extends GsrsVersionedRepository<ImportMetadata, UUID> {

    /*
    'clearAutomatically' makes these methods work correctly -- without it, they fail silently
     */
    @Modifying(clearAutomatically = true)
    @Query("update ImportMetadata i set validationStatus= ?2 where i.recordId = ?1")
    public void updateRecordValidationStatus(UUID recordId, ImportMetadata.RecordValidationStatus status);

    @Modifying(clearAutomatically = true)
    @Query("update ImportMetadata i set versionStatus= ?2 where i.recordId = ?1")
    public void updateRecordVersionStatus(UUID recordId, ImportMetadata.RecordVersionStatus status);

    @Modifying(clearAutomatically = true)
    @Query("update ImportMetadata i set processStatus= ?2 where i.recordId = ?1")
    public void updateRecordProcessStatus(UUID recordId, ImportMetadata.RecordProcessStatus status);

    @Modifying(clearAutomatically = true)
    @Query("update ImportMetadata i set importStatus= ?2 where i.recordId = ?1")
    public void updateRecordImportStatus(UUID recordId, ImportMetadata.RecordImportStatus status);

    @Query("select d from ImportMetadata d where d.recordId = ?1 and d.version = ?2")
    public ImportMetadata retrieveByIDAndVersion(UUID id, int version);

    @Query("select d from ImportMetadata d where d.instanceId = ?1")
    public ImportMetadata retrieveByInstanceID(UUID id);

    @Query("select d from ImportMetadata d where d.recordId = ?1")
    public ImportMetadata retrieveByRecordID(UUID id);

    @Modifying
    @Transactional
    @Query("delete from ImportMetadata i where i.recordId = ?1")
    void deleteByRecordId(UUID recordId);

    @Modifying
    @Transactional
    @Query("delete from ImportMetadata i where i.recordId = ?1 and version=?2")
    void deleteByRecordIdAndVersion(UUID recordId, int version);

    @Modifying(clearAutomatically = true)
    @Query("update ImportMetadata i set version = version +1 where i.recordId = ?1")
    public void incrementVersion(UUID recordId);

    @Modifying(clearAutomatically = true)
    @Query("update ImportMetadata i set instanceId= ?1 where i.recordId = ?2")
    public void setInstanceIdForRecord(UUID newInstanceId, UUID recordId);

    @Query("select i.recordId from ImportMetadata i")
    List<UUID> getAllRecordIds();

}
