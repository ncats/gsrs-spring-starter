package gsrs.repository;

import ix.core.models.BackupEntity;
import ix.core.models.ETag;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Repository
public interface BackupRepository extends GsrsRepository<BackupEntity, Long> {

    Optional<BackupEntity> findByRefid(String refid);
    @Query("select e from BackupEntity e")
    Stream<BackupEntity> streamAll();

    Stream<BackupEntity> findAllByKindIn(List<String> kinds);

    long countAllByKindIn(List<String> kinds);
}
