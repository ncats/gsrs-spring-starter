package gsrs.security;

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

@Component
public class LoginAndLogoutEventListener {

    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;

    @EventListener
    @Transactional
    public void onLogin(AuthenticationSuccessEvent event) {
        UserProfile up = (UserProfile) event.getAuthentication().getPrincipal();

        System.out.println("Logged in user " + up);

        List<Session> sessions = sessionRepository.getActiveSessionsFor(up);
        if(sessions.isEmpty()){
            //make new one?
            Session s = new Session();
            //this is so we don't have a stale entity

            s.profile = userProfileRepository.findByUser_Username(up.user.username);
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
