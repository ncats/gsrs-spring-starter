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

    /**
     * Get the list of admin usernames.
     * @apiNote This should not be directly called by users.
     * @param paging
     * @return
     * @see #getAdminName()
     */
    @Query("select username from Principal  where admin=true")
    List<String> _getAdminNames(Pageable paging);

    /**
     * Get an admin name this will only return at most 1 admin username
     * and is not guarenteed to always return the same one.
     * @return empty if there are no admins in the database.
     */
    default Optional<String> getAdminName(){
        List<String> names = _getAdminNames(PageRequest.of(0,1));
        if(names.isEmpty()){
            return Optional.empty();
        }
        //name[0] shouldn't be null... we can't have null usernames right?
        return Optional.of(names.get(0));
    }

}
