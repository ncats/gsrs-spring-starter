package gsrs.repository;


import ix.core.models.Principal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Repository
public interface PrincipalRepository extends GsrsRepository<Principal, Long> {

    Principal findDistinctByUsernameIgnoreCase(String username);



}
