package gsrs.security;

import gsrs.cache.GsrsCache;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.security.SessionConfiguration;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import gov.nih.ncats.common.util.TimeUtil;

public class LegacyGsrsAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private SessionConfiguration sessionConfiguration;


    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
            System.out.println("========== Legacy ... onAuthenticationSuccess 1 =======");
        // So, this method only applies to token auth.
        // However, a session is created/adjusted following token auth
        if( !(authentication instanceof AbstractGsrsAuthenticationToken) ) {
            return;
        }
        System.out.println("Legacy ... onAuthenticationSuccess 2");

        AbstractGsrsAuthenticationToken authenticationToken =    (AbstractGsrsAuthenticationToken) authentication;
        UserProfile up =  Optional.ofNullable(userProfileRepository.findByUser_UsernameIgnoreCase(authenticationToken.getUserProfile().getIdentifier()))
                .map(oo->oo.standardize())
                .orElse(null);

        // Not sure this will work yet
        Optional<Session> session = sessionConfiguration.cleanUpSessionsThenGetSession(up);

        // Add a session cookie
        Cookie sessionCookie = new Cookie( sessionConfiguration.sessionCookieName(), session.map(ss->ss.id.toString()).orElse(null));
        sessionCookie.setHttpOnly(true);
        if(sessionConfiguration.sessionCookieSecure()) {
            sessionCookie.setSecure(true);
        }
        response.addCookie( sessionCookie );
//        gsrsCache.setRaw(id, session.id);
        // call the original impl
        super.onAuthenticationSuccess( request, response, authentication );
    }
}