package gsrs.security;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.cache.GsrsCache;
import gsrs.repository.PrincipalRepository;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Principal;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import ix.utils.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

//@Component
public class LegacyAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private LegacyAuthenticationConfiguration authenticationConfiguration;
    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private UserTokenCache userTokenCache;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private GsrsCache gsrsCache;


    @Value("${gsrs.sessionKey}")
    private String sessionCookieName;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = null;
        Cookie[] allCookies = request.getCookies();
        if (allCookies != null) {
            Cookie sessionCookie =
                    Arrays.stream(allCookies).filter(x -> x.getName().equals(sessionCookieName))
                            .findFirst().orElse(null);
            if(sessionCookie !=null){
                String id = sessionCookie.getValue();
                UUID cachedSessionId = (UUID) gsrsCache.getRaw(id);
                if(cachedSessionId !=null) {
                    TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
                    auth = transactionTemplate.execute(status ->{
                        Session session = sessionRepository.findById(cachedSessionId).orElse(null);
                        if(session !=null && !session.expired){
                            session.accessed = TimeUtil.getCurrentTimeMillis();
                            return new SessionIdAuthentication(session.profile, id);
                        }
                        return null;
                    });

                }
            }

        }
//    }
//
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //the order of the old GSRS is to check this:
        /*
        AuthenticatorFactory authFac = AuthenticatorFactory.getInstance(app);
		authFac.registerAuthenticator(new TrustHeaderAuthenticator());
		authFac.registerAuthenticator(new UserPasswordAuthenticator());
		authFac.registerAuthenticator(new UserTokenAuthenticator());
		authFac.registerAuthenticator(new UserKeyAuthenticator());
         */
//        HttpServletRequest request = (HttpServletRequest) servletRequest;

        if(auth !=null && authenticationConfiguration.isTrustheader()){

            String username = request.getHeader(authenticationConfiguration.getUsernameheader());
            String email = request.getHeader(authenticationConfiguration.getUseremailheader());
            if(username !=null && email !=null){
                UserProfile up = repository.findByUser_Username(username);
                if(up ==null && authenticationConfiguration.isAutoregister()){
                    Principal p = new Principal(username, email);
                    up = new UserProfile(p);
                    if(authenticationConfiguration.isAutoregisteractive()){
                        up.active = true;
                    }
                    up.systemAuth = false;
                    //should cascade new Principal
                    repository.saveAndFlush(up);
                }
                if(up !=null){
                    auth = new UserProfilePasswordAuthentication(up);
                }
            }
        }
        /*
        String user=r.getHeader("auth-username");
        String key=r.getHeader("auth-key");
         */
        if(auth ==null) {
            String username = request.getHeader("auth-username");
            String pass = request.getHeader("auth-password");
            if (username != null && pass != null) {
                UserProfile up = repository.findByUser_Username(username);
                if(up ==null && authenticationConfiguration.isAutoregister()) {
                    Principal p = new Principal(username, null);
                    up = new UserProfile(p);
                    if (authenticationConfiguration.isAutoregisteractive()) {
                        up.active = true;
                    }
                    up.systemAuth = false;
                    //should cascade new Principal
                    repository.saveAndFlush(up);

                }
                if(up!=null){
                    if(up.acceptPassword(pass)){
                        //valid password!
                        auth = new UserProfilePasswordAuthentication(up);

                    }else{
                        throw new BadCredentialsException("invalid credentials for username" + username);
                    }

                }
            }
        }
        if(auth ==null) {
            String token = request.getHeader("auth-token");
            if(token !=null){

                auth = new LegacyUserTokenAuthentication(userTokenCache.getUserProfileFromToken(token), token);
            }
        }
        if(auth !=null) {
            //add a new Session each time !?

            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);

    }
}
