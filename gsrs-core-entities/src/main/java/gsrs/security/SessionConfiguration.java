package gsrs.security;

import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.repository.SessionRepository;
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
@Accessors(fluent = true)
public class SessionConfiguration {

    @Autowired
    private SessionRepository sessionRepository;

    // gsrs.sessionExpirationMS ==> gsrs.sessions.sessionExpirationMS
    // gsrs.sessionKey ==> gsrs.sessions.sessionCookieName
    // gsrs.sessionSecure ==> gsrs.sessions.sessionCookieSecure

    private Long sessionExpirationMS = -1L;
    private String sessionCookieName  = "ix.session";
    private Boolean sessionCookieSecure = true;
    // private String logPath = "logs";

    public Long calculateSessionExpirationDelta(){
        return (sessionExpirationMS==null || sessionExpirationMS<=0)?Long.MAX_VALUE:sessionExpirationMS;
    }

    public Optional<Session> cleanUpSessionsThenGetSession(UserProfile up) {
        if(up==null)return Optional.empty();
        List<Session> sessions = sessionRepository.getActiveSessionsFor(up);

        sessions = sessions.stream()
                .filter(s->{
                    if(TimeUtil.getCurrentTimeMillis() > s.created + calculateSessionExpirationDelta()){
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
            //this is so we don't have a stale entity
            s.profile = Optional.ofNullable(up)
                    .map(oo->oo.standardize())
                    .orElse(null);
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
