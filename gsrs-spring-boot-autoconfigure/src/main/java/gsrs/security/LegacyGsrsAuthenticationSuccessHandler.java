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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        // So, this method only applies to token auth.
        // However, a session is created/adjusted following token auth
        if( !(authentication instanceof AbstractGsrsAuthenticationToken) ) {
            return;
        }

        AbstractGsrsAuthenticationToken authenticationToken =    (AbstractGsrsAuthenticationToken) authentication;
        UserProfile up =  Optional.ofNullable(userProfileRepository.findByUser_UsernameIgnoreCase(authenticationToken.getUserProfile().getIdentifier()))
                .map(oo->oo.standardize())
                .orElse(null);

        Optional<Session> session = sessionConfiguration.cleanUpSessionsThenGetSession(up);

        // Add a session cookie
        Cookie sessionCookie = new Cookie( sessionConfiguration.getSessionCookieName(), session.map(ss->ss.id.toString()).orElse(null));
        sessionCookie.setHttpOnly(true);
        if(sessionConfiguration.getSessionCookieSecure()) {
            sessionCookie.setSecure(true);
        }
        response.addCookie( sessionCookie );
//        gsrsCache.setRaw(id, session.id);
        // call the original impl
        super.onAuthenticationSuccess( request, response, authentication );
    }
}
