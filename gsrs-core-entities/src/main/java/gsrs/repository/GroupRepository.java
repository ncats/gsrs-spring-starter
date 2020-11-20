package gsrs.repository;


import ix.core.models.Group;
import ix.core.models.Principal;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends GsrsRepository<Group, Long> {

    Group findByNameIgnoreCase(String name);

    List<Group> findGroupsByMembers(Principal member);
}
