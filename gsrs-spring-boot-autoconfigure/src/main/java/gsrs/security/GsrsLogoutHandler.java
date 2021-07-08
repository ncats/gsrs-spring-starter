package gsrs.security;

import java.util.ArrayList;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import gsrs.repository.SessionRepository;
import ix.core.models.Session;
import ix.core.models.UserProfile;
@Component
public class GsrsLogoutHandler implements LogoutHandler {

    @Autowired(required = false)
    private UserTokenCache userTokenCache;

    @Autowired
    private SessionRepository sessionRepository;
    
    @Value("${server.servlet.session.cookie.name}")
    private String sessionCookieName;
    
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
                Cookie cookie = new Cookie(sessionCookieName, null);
                cookie.setMaxAge(-1);
                
                cookie.setHttpOnly(true);

              //cookie.setSecure(true);
                
                //add cookie to response
                httpServletResponse.addCookie(cookie);
            }

        }
    }
}
