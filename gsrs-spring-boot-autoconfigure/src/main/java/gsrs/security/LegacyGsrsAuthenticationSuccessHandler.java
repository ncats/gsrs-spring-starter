package gsrs.security;

import gsrs.cache.GsrsCache;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
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
    private SessionRepository sessionRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private GsrsCache gsrsCache;

	
    @Value("#{new Long('${gsrs.sessionExpirationMS:-1}')}")
    private Long sessionExpirationMS;
	
    //TODO this is the default session cookie name Spring uses or should we just use ix.session
    @Value("${gsrs.sessionKey}")
    private String sessionCookieName;
    private String logPath = "logs";
    @Value("#{new Boolean('${gsrs.sessionSecure:true}')}")
    private Boolean sessionCookieSecure;

    public String getSessionCookieName() {
        return sessionCookieName;
    }

    public void setSessionCookieName(String sessionCookieName) {
        this.sessionCookieName = sessionCookieName;
    }

    public Boolean getSessionCookieSecure() {
        return sessionCookieSecure;
    }

    public void setSessionCookieSecure(Boolean sessionCookieSecure) {
        this.sessionCookieSecure = sessionCookieSecure;
    }

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
	    
	    
        String id =null;
        Session session=null;
        if(sessions.isEmpty()){
            //create new session
            Session s = new Session(up);
            //????????!!!!
            //up.active= true;
            sessionRepository.saveAndFlush(s);
            id = s.id.toString();
            session = s;
        }else{
            //???
            for(Session s : sessions){
                id = s.id.toString();
                session = s;
		break;
            }
        }

        // Add a session cookie
        Cookie sessionCookie = new Cookie( sessionCookieName, id );
        sessionCookie.setHttpOnly(true);
        if(sessionCookieSecure==null || sessionCookieSecure.booleanValue()) {
            sessionCookie.setSecure(true);
        }
        response.addCookie( sessionCookie );
//        gsrsCache.setRaw(id, session.id);
        // call the original impl
        super.onAuthenticationSuccess( request, response, authentication );
    }
}
