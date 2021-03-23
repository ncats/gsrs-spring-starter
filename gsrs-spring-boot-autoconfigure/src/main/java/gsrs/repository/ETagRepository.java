package gsrs.repository;

import ix.core.models.ETag;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ETagRepository extends GsrsRepository<ETag, Long> {
    Optional<ETag> findByEtag(String Etag);
}
