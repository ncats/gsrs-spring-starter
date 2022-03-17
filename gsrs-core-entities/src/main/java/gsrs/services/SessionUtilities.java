package gsrs.services;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.repository.SessionRepository;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import org.springframework.context.ApplicationContextAware;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SessionUtilities {

    public static Optional<Session> cleanUpSessionsThenGetSession(UserProfile up, SessionRepository sessionRepository, Long sessionExpirationMS) {
        long expDelta = (sessionExpirationMS==null || sessionExpirationMS<=0)?Long.MAX_VALUE:sessionExpirationMS;
        List<Session> sessions = sessionRepository.getActiveSessionsFor(up);

        sessions = sessions.stream()
                .filter(s->{
                    if(TimeUtil.getCurrentTimeMillis() > s.created + expDelta){
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
