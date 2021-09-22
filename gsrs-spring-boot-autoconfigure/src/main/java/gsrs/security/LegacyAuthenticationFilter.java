package gsrs.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import gov.nih.ncats.common.stream.StreamUtil;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.cache.GsrsCache;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import lombok.extern.slf4j.Slf4j;

//@Component
@Slf4j
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
    
    private void logHeaders(HttpServletRequest req){
        log.debug("HEADERS ON REQUEST ===================");
        StringBuilder allheaders=new StringBuilder();
        StreamUtil.forEnumeration(req.getHeaderNames()).forEach(head->{
            allheaders.append(head + "\t" + req.getHeader(head) + "\n");
        });
        log.debug(allheaders.toString());
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if(authenticationConfiguration.isLogheaders()) {
            logHeaders(request);
        }
        
        Authentication auth = null;
        Cookie[] allCookies = request.getCookies();
        if (allCookies != null) {
            Cookie sessionCookie =
                    Arrays.stream(allCookies).filter(x -> x.getName().equals(sessionCookieName))
                            .findFirst().orElse(null);
            if(sessionCookie !=null){
                String id = sessionCookie.getValue();
                UUID cachedSessionIdt = null;
                try {
                    cachedSessionIdt = UUID.fromString(id);
                }catch(Exception e) {
                    
                }
//                UUID cachedSessionId = (UUID) gsrsCache.getRaw(id);
                if(cachedSessionIdt !=null) {
                    UUID cachedSessionId = cachedSessionIdt;
                    
                    TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
                    auth = transactionTemplate.execute(status ->{
                        Session session = sessionRepository.findById(cachedSessionId).orElse(null);
                        if(session !=null && !session.expired){
                            //Do we need to save this?
                            session.accessed = TimeUtil.getCurrentTimeMillis();
                            System.out.println("Found:" + cachedSessionId);
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
        if(authenticationConfiguration.isLogheaders()) {
            log.debug("After cookie authentication, auth is:" + auth);
        }
        
        // TODO: TP changed below, but needs feedback
        // why != null? that doesn't sound right ...
        // changing to ==null
        if(auth ==null && authenticationConfiguration.isTrustheader()){
            if(authenticationConfiguration.isLogheaders()) {
                log.debug("Trust Headers is on");
            }

            String username = Optional.ofNullable(authenticationConfiguration.getUsernameheader())
                                      .map(e->request.getHeader(e))
                                      .orElse(null);
            String email =    Optional.ofNullable(authenticationConfiguration.getUseremailheader())
                                      .map(e->request.getHeader(e))
                                      .orElse(null);
            List<Role> roles =    Optional.ofNullable(authenticationConfiguration.getUserrolesheader())
                    .map(e->request.getHeader(e))
                    .map(v->Arrays.stream(v.split(";"))
                                  .map(r->r.trim())
                                  .map(r->Role.valueOf(r))
                                  .collect(Collectors.toList())
                        )
                    .orElse(null);
            
                    
            if(username !=null){
                if(authenticationConfiguration.isLogheaders()) {
                    log.debug("Trust USER IS:" + username);
                }
                UserProfile up =  Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(username))
                        .map(oo->oo.standardize())
                        .orElse(null);
                if(up ==null && authenticationConfiguration.isAutoregister()){
                    Principal p = new Principal(username, email);
                    up = new UserProfile(p);
                    if(authenticationConfiguration.isAutoregisteractive()){
                        up.active = true;
                    }
                    up.setRoles(roles);
                    
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
                UserProfile up = 
                        Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(username))
                        .map(oo->oo.standardize())
                        .orElse(null);
                if(up ==null && authenticationConfiguration.isAutoregister()) {
                    Principal p = new Principal(username, null).standardize();
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
                UserProfile up = userTokenCache.getUserProfileFromToken(token);
                if(up!=null) {
                    auth = new LegacyUserTokenAuthentication(up, token);
                }
            }
        }
        if(auth !=null) {
            //add a new Session each time !?
            
            //TODO: perhaps allow a short-circuit here if auth is outsourced

            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);

    }
}
