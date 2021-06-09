package gsrs.repository;

import ix.core.models.Namespace;
import ix.core.models.Principal;
import org.springframework.stereotype.Repository;

@Repository
public interface NamespaceRepository extends GsrsRepository<Namespace, Long> {
    Namespace findByName(String name);
}
