package gsrs.repository;

import ix.core.models.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.stream.Stream;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    UserProfile findByUser_Username(String username);

    UserProfile findByKey(String key);

    @Query("select e from UserProfile e")
    Stream<UserProfile> streamAll();

    @Query("select e.user.username as username, e.key from UserProfile e")
    Stream<UserTokenInfo> streamAllTokenInfo();

    //this.user.username + this.key);

    interface UserTokenInfo{
         String getUsername();
         String getKey();
    }
}
