package gsrs.repository;

import ix.core.models.Namespace;
import ix.core.models.Principal;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NamespaceRepository extends GsrsRepository<Namespace, Long> {
    Namespace findByName(String name);
    
    @Query("select ns.id from Namespace ns")
    List<Long> getAllIDs();
}
