package gsrs.repository;

import ix.core.models.Session;
import ix.core.models.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    @Query("select s from Session s where s.profile=?1 and s.expired=false")
    List<Session> getActiveSessionsFor(UserProfile up);
    @Query("select s.profile from Session s where s.id=?1 and s.expired=false")
    UserProfile findUserProfileByUnexpiredSessionId(UUID sessionId);
}
