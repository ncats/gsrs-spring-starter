package gsrs.repository;


import ix.core.models.Principal;
import org.springframework.stereotype.Repository;

@Repository
public interface PrincipalRepository extends GsrsRepository<Principal, Long> {

    Principal findDistinctByUsernameIgnoreCase(String username);



}
