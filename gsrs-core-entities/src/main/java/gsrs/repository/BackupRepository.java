package gsrs.repository;

import ix.core.models.BackupEntity;
import ix.core.models.ETag;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BackupRepository extends GsrsRepository<BackupEntity, Long> {

    Optional<BackupEntity> findByRefid(String refid);

}
