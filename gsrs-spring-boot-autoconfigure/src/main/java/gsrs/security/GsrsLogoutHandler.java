package gsrs.security;

import gsrs.repository.SessionRepository;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
@Component
public class GsrsLogoutHandler implements LogoutHandler {

    @Autowired(required = false)
    private UserTokenCache userTokenCache;

    @Autowired
    private SessionRepository sessionRepository;

    @Override
    @Transactional
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        if(authentication instanceof UserProfilePasswordAuthentication) {
            UserProfile up = ((UserProfilePasswordAuthentication) authentication).getPrincipal();
            if(up !=null) {
                for (Session s : new ArrayList<>(sessionRepository.getActiveSessionsFor(up))) {
                    s.expired = true;
                    sessionRepository.saveAndFlush(s);
                }
                userTokenCache.evictUser(up);
            }

        }
    }
}
