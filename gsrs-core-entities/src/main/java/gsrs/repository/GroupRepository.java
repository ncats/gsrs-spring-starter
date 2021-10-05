package gsrs.repository;


import ix.core.models.Group;
import ix.core.models.Principal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends GsrsRepository<Group, Long> {

    default Group findByName(String name) {
        return this.findByNameIgnoreCase(name);
    }
    Group findByNameIgnoreCase(String name);

    List<Group> findGroupsByMembers(Principal member);
    @Query("select g.name from Group g")
    List<String> findAllGroupNames();
}
