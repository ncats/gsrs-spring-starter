package gsrs.holdingArea.repository;

import gsrs.holdingArea.model.ImportMetadata;
import gsrs.repository.GsrsVersionedRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImportMetadataRepository extends GsrsVersionedRepository<ImportMetadata, UUID> {

    /*
    'clearAutomatically' makes these methods work correctly -- without it, they fail silently
     */
    @Modifying(clearAutomatically = true)
    @Query("update ImportMetadata i set validationStatus= ?2 where i.instanceId = ?1")
    public void updateRecordValidationStatus(UUID instanceId, ImportMetadata.RecordValidationStatus status);

    @Modifying(clearAutomatically = true)
    @Query("update ImportMetadata i set versionStatus= ?2 where i.instanceId = ?1")
    public void updateRecordVersionStatus(UUID instanceId, ImportMetadata.RecordVersionStatus status);

    @Modifying(clearAutomatically = true)
    @Query("update ImportMetadata i set processStatus= ?2 where i.instanceId = ?1")
    public void updateRecordProcessStatus(UUID instanceId, ImportMetadata.RecordProcessStatus status);

    @Modifying(clearAutomatically = true)
    @Query("update ImportMetadata i set importStatus= ?2 where i.instanceId = ?1")
    public void updateRecordImportStatus(UUID instanceId, ImportMetadata.RecordImportStatus status);

    @Query("select d from ImportMetadata d where d.recordId = ?1 and d.version = ?2")
    public ImportMetadata retrieveByIDAndVersion(UUID id, int version);

}
