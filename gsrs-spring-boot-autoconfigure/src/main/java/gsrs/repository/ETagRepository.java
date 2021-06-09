package gsrs.repository;

import ix.core.models.ETag;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ETagRepository extends GsrsRepository<ETag, Long> {
    @Query("select e from ETag e where e.etag =?1")
    Optional<ETag> findByEtag(String Etag);

}
