package gsrs.stagingarea.repository;

import gsrs.dataexchange.model.ImportProcessingJob;
import gsrs.repository.GsrsRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@Transactional
public interface ImportProcessingJobRepository extends GsrsRepository<ImportProcessingJob, UUID> {

    @Transactional
    @Query("update ImportProcessingJob i set completedRecordCount = ?2 where i.id = ?1")
    @Modifying(clearAutomatically = true)
    void updateCompletedRecordCount(UUID Id, int count);
}
