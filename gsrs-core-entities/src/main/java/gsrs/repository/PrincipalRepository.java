package gsrs.repository;


import ix.core.models.Principal;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public interface PrincipalRepository extends GsrsRepository<Principal, Long> {

    Principal findDistinctByUsernameIgnoreCase(String username);


}
