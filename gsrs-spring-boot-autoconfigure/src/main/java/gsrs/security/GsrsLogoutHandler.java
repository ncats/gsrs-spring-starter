package gsrs.security;

import java.util.ArrayList;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private SessionConfiguration sessionConfiguration;
    
    @Override
    @Transactional
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {

        if(authentication instanceof AbstractGsrsAuthenticationToken) {
            UserProfile up = ((AbstractGsrsAuthenticationToken) authentication).getUserProfile();

            if(up !=null) {
                for (Session s : new ArrayList<>(sessionRepository.getActiveSessionsFor(up))) {

                    s.expired = true;
                    s.setIsDirty("expired");
                    sessionRepository.saveAndFlush(s);
                }

                // Commenting out to allow valid computedTokens to work if session has expired.
                // userTokenCache.evictUser(up);

                // The following 3 cookies are present to ensure logout works
                // TODO: figure out path and secure settings if necessary

                Cookie cookie = new Cookie(sessionConfiguration.getSessionCookieName(), null);
                cookie.setMaxAge(0);
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                //add cookie to response
                httpServletResponse.addCookie(cookie);

                Cookie cookie2 = new Cookie(sessionConfiguration.getSessionCookieName(), null);
                cookie2.setMaxAge(0);                
                cookie2.setHttpOnly(true);
                cookie2.setPath("/api/v1");
                //add cookie to response
                httpServletResponse.addCookie(cookie2);

                Cookie cookie3 = new Cookie(sessionConfiguration.getSessionCookieName(), null);
                cookie3.setMaxAge(0);                
                cookie3.setHttpOnly(true);
                //add cookie to response
                httpServletResponse.addCookie(cookie3);
            }

        }
    }
}
