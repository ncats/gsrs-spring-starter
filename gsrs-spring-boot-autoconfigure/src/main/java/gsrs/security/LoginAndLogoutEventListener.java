package gsrs.security;

import gsrs.cache.GsrsCache;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.services.SessionUtilities;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import gov.nih.ncats.common.util.TimeUtil;
import java.util.List;
import java.util.Optional;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class LoginAndLogoutEventListener {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private GsrsCache gsrsCache;

    @Value("#{new Long('${gsrs.sessionExpirationMS:-1}')}")
    private Long sessionExpirationMS;

    @EventListener
    @Transactional
    public void onLogin(AuthenticationSuccessEvent event) {
        // This method is called:
        // after user credentials are checked successfully
        System.out.println("========== Event ... onLogin 1 =======");

        UserProfile up = (UserProfile) event.getAuthentication().getPrincipal();

        // When trying to use SessionUtilities.java I get this exception
        // org.hibernate.PersistentObjectException: detached entity passed to persist: ix.core.models.UserProfile
        // Optional<Session> session = SessionUtilities.cleanUpSessionsThenGetSession(up, sessionRepository, sessionExpirationMS);

        List<Session> sessions = sessionRepository.getActiveSessionsFor(up);

        long expDelta = (sessionExpirationMS==null || sessionExpirationMS<=0)?Long.MAX_VALUE:sessionExpirationMS;

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

            s.profile = Optional.ofNullable(userProfileRepository.findByUser_UsernameIgnoreCase(up.user.username))
                    .map(oo->oo.standardize())
                    .orElse(null);

            sessionRepository.saveAndFlush(s);
        }else{
            long time = System.currentTimeMillis();
            for(Session s : sessions){
                s.accessed = time;
            }
            sessionRepository.saveAll(sessions);
        }
    }
}