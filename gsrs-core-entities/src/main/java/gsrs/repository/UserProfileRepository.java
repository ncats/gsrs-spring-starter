package gsrs.repository;

import ix.core.models.Role;
import ix.core.models.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    @Deprecated
    UserProfile findByUser_Username(String username);
    
    UserProfile findByUser_UsernameIgnoreCase(String username);
    /**
     * Get the candidate list of admin.  This is a very efficient
     * query but it might get back profiles that aren't actually admins.
     * The {@link UserProfile#getRoles()} is an expensive operation
     * so this should be used as a pre-filter before that method is called.
     *
     * @apiNote This should not be directly called by users.
     * @return the list of UserProfiles that might be admins.
     * @see #findAnAdminUsername()
     */
    @Query("select up from UserProfile up where LOCATE('Admin', CAST(up.rolesJSON as string)) > 0")
    List<UserProfile> _findAllCandidateAdmins();
    /**
     * Get an admin name this will only return at most 1 admin username
     * and is not guaranteed to always return the same one.
     * @return empty if there are no admins in the database.
     */
    @Transactional(readOnly = true)
    default Optional<String> findAnAdminUsername(){
        return _findAllCandidateAdmins().stream()
                        .filter(up-> {
                            if(up.user ==null || up.user.username ==null){
                                return false;
                            }
                            List<Role> roles = up.getRoles();
                            if(roles ==null){
                                return false;
                            }
                            return roles.contains(Role.Admin);
                        })
                .map(up-> up.user.username)
                .findAny();

    }
    UserProfile findByKey(String key);

    @Query("select e from UserProfile e")
    Stream<UserProfile> streamAll();

    @Query("select e.user.username as username, e.key from UserProfile e")
    Stream<UserTokenInfo> streamAllTokenInfo();

    @Query("select e.user.username as username, e.user.email as email, e.user.created," +
            "e.user.modified, e.id, e.active from UserProfile e")
    List<UserProfileSummary> listSummary();

    interface UserTokenInfo{
         String getUsername();
         String getKey();
    }

    interface UserProfileSummary{
        String getUsername();
        String getEmail();
        Date getCreated();
        Date getModified();
        Long getId();
        Boolean getActive();
    }
}
