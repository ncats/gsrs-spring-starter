package gsrs.repository;


import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import ix.core.models.Edit;
import ix.core.util.EntityUtils.Key;

@Repository
public interface EditRepository extends GsrsRepository<Edit, UUID> {

    List<Edit> findByRefidOrderByCreatedDesc(String refId);
    
    //TODO: we need this to actually include the kind, because not all refIDs are
    //globally unique
    Optional<Edit> findFirstByRefidOrderByCreatedDesc(String refId);   

    List<Edit> findByRefidAndVersion(String refId, String version);
    
    default Optional<Edit> findFirstByRefidOrderByCreatedDescAndKinds(String refId, Set<String> kinds){
        return findByRefidOrderByCreatedDesc(refId)
                .stream()
                .filter(e->kinds.contains(e.kind))
                .findFirst();
    }
    
    default Optional<Edit> findByRefidAndVersionAndKinds(String refId, String version, Set<String> kinds){
        return findByRefidAndVersion(refId,version)
                .stream()
                .filter(e->kinds.contains(e.kind))
                .findFirst();
    }
    default Optional<Edit> findByKeyAndVersion(Key k, String version){
        Set<String> kinds = k.getEntityInfo().getInherittedRootEntityInfo().getTypeAndSubTypes()
                .stream().map(ei->ei.getEntityClass().getName()).collect(Collectors.toSet());
        
        return findByRefidAndVersionAndKinds(k.getIdString(),version, kinds);
    }
    
    default Optional<Edit> findFirstByKeyOrderByCreatedDesc(Key k){
        Set<String> kinds = k.getEntityInfo().getInherittedRootEntityInfo().getTypeAndSubTypes()
                .stream().map(ei->ei.getEntityClass().getName()).collect(Collectors.toSet());
        
        return findFirstByRefidOrderByCreatedDescAndKinds(k.getIdString(),kinds);
    }
    
}
