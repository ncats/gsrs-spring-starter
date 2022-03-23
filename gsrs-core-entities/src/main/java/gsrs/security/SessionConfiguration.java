package gsrs.security;

import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import ix.utils.Util;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
@ConfigurationProperties(prefix = "gsrs.sessions")
@Data
public class SessionConfiguration {
            
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;

    // gsrs.sessionExpirationMS ==> gsrs.sessions.sessionExpirationMS
    // gsrs.sessionKey ==> gsrs.sessions.sessionCookieName
    // gsrs.sessionSecure ==> gsrs.sessions.sessionCookieSecure

    private static final long MAX_SESSION_DURATION = 31557600000L; // 365 days

    // A negative number results sessions that never expire!
    private Long sessionExpirationMS = -1L;  // 60000 is one minute
    private String sessionCookieName  = "ix.session";
    private Boolean sessionCookieSecure = true;
    // private String logPath = "logs";

    private Long calculateSessionExpirationDelta(){
        return (sessionExpirationMS==null || sessionExpirationMS<=0 || sessionExpirationMS > MAX_SESSION_DURATION)?MAX_SESSION_DURATION:sessionExpirationMS;
    }

    public boolean isExpired(Session session){
        if(session.expired)return true;
        if(sessionExpirationMS<=0)return false;
        if(session.created + calculateSessionExpirationDelta() > TimeUtil.getCurrentTimeMillis()) {
            return false;
        }
        return true;
    }

    public Optional<Session> cleanUpSessionsThenGetSession(UserProfile up) {
        if(up==null)return Optional.empty();
        List<Session> sessions = sessionRepository.getActiveSessionsFor(up);

        sessions = sessions.stream()
                .filter(s->{
                    if(isExpired(s)){
                        s.expired = true;
                        s.setIsDirty("expired");
                        sessionRepository.saveAndFlush(s);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if(sessions.isEmpty()){
            //make new one?
            Session s = new Session();

            s.profile = Optional.ofNullable(userProfileRepository.findByUser_UsernameIgnoreCase(up.user.username))
                    .map(oo->oo.standardize())
                    .orElse(null);
/*
            //this is so we don't have a stale entity
            s.profile = Optional.ofNullable(up)
                    .map(oo->oo.standardize())
                    .orElse(null);
*/
            sessionRepository.saveAndFlush(s);
            return Optional.of(s);
        }else{
            long time = TimeUtil.getCurrentTimeMillis();
            for(Session s : sessions){
                s.accessed = time;
            }
            sessionRepository.saveAll(sessions);
        }
        return sessions.stream().findFirst();
    }
}
