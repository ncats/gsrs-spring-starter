package gsrs.stagingarea.repository;

import gsrs.dataexchange.model.ImportProcessingJob;
import gsrs.repository.GsrsRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@Transactional
public interface ImportProcessingJobRepository extends GsrsRepository<ImportProcessingJob, UUID> {

}
