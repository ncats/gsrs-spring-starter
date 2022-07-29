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

}
