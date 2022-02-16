package gsrs.security;

import gsrs.cache.GsrsCache;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class LoginAndLogoutEventListener {

    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private GsrsCache gsrsCache;

    @EventListener
    @Transactional
    public void onLogin(AuthenticationSuccessEvent event) {
        UserProfile up = (UserProfile) event.getAuthentication().getPrincipal();


        List<Session> sessions = sessionRepository.getActiveSessionsFor(up);
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
