package gsrs.repository;


import org.springframework.stereotype.Repository;

import ix.core.models.Principal;


@Repository
public interface PrincipalRepository extends GsrsRepository<Principal, Long> {

    Principal findDistinctByUsernameIgnoreCase(String username);
}
