package gsrs.repository;


import ix.core.models.Edit;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EditRepository extends GsrsRepository<Edit, UUID> {

    List<Edit> findByRefidOrderByCreatedDesc(String refId);
}
