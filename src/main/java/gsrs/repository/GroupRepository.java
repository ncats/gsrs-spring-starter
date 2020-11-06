package gsrs.repository;


import ix.core.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends GsrsRepository<Group, Long> {

    Group findByNameIgnoreCase(String name);
}
