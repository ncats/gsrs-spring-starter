package gsrs.security;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import gsrs.model.UserProfileAuthenticationResult;
import gsrs.services.PrivilegeService;
import org.apache.tomcat.util.http.parser.Authorization;
import org.springframework.beans.factory.annotation.Autowired;
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
    SessionConfiguration sessionConfiguration;

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

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private PrivilegeService privilegeService;

    private void logHeaders(HttpServletRequest req){
        if(log.isDebugEnabled()) {
            log.debug("HEADERS ON REQUEST ===================");
            StringBuilder allheaders = new StringBuilder();
            StreamUtil.forEnumeration(req.getHeaderNames()).forEach(head -> {
                allheaders.append(head + "\t" + req.getHeader(head) + "\n");
            });
            log.debug(allheaders.toString());
        }
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
                    Arrays.stream(allCookies).filter(x -> x.getName().equals(sessionConfiguration.getSessionCookieName()))
                            .findFirst().orElse(null);
            if(sessionCookie !=null){
                String id = sessionCookie.getValue();
                UUID cachedSessionIdt = null;
                try {
                    cachedSessionIdt = UUID.fromString(id);
                }catch(Exception e) {

                }
                if(cachedSessionIdt !=null) {
                    UUID cachedSessionId = cachedSessionIdt;

                    TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
                    auth = transactionTemplate.execute(status ->{
                        Session session = sessionRepository.findById(cachedSessionId).orElse(null);
                        if(session !=null && !sessionConfiguration.isExpired(session)){
                            //Do we need to save this?
                            session.accessed = TimeUtil.getCurrentTimeMillis();
                            request.getSession().setAttribute("username", session.profile.getIdentifier());
                            List<String> privileges = privilegeService.getPrivilegesForRoles(session.profile.getRoles());
                            return new SessionIdAuthentication(session.profile, id, privileges);
                        }
                        return null;
                    });

                }
            }

        }

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
                            .filter(r->Arrays.stream(PrivilegeService.instance().getAllRoleNames().toArray(new String[0])).anyMatch(r.trim()::equals))
                            .map(r->Role.of(r.trim()))
                            .distinct()
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
                    up = autoregisterNewUser(username, email, roles);
                }
                if(up !=null && up.active){
                    //privileges handled by UserProfilePasswordAuthentication's constructor
                    auth = new UserProfilePasswordAuthentication(up);
                }
            }
        }
        if(auth ==null) {
            String username = request.getHeader("auth-username");
            String pass = request.getHeader("auth-password");
            if (username != null && pass != null) {
                UserProfile up =
                        Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(username))
                                .map(oo->oo.standardize())
                                .orElse(null);
                if(up ==null && authenticationConfiguration.isAutoregister()) {
                    up = autoregisterNewUser(username);

                }
                if(up!=null && up.active){
                    UserProfileAuthenticationResult authenticationResult =up.acceptPassword(pass);
                    if(authenticationResult.matchesRepository()){
                        //valid password!
                        if(authenticationResult.needsSave()){
                            UserProfile finalUp = up;
                            TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
                            UserProfile savedUp=transactionTemplate.execute(status -> {
                                Optional<UserProfile> opt = Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(finalUp.user.username));
                                opt.get().setPassword(pass);
                                saveUserProfile(opt.get());
                                return opt.get();
                            });
                            auth = new UserProfilePasswordAuthentication(savedUp);
                        } else {
                            auth = new UserProfilePasswordAuthentication(up);
                        }
                    }else{
                        throw new BadCredentialsException("invalid credentials for username: " + username);
                    }

                }
            }
        }
        if(auth ==null) {
            String token = request.getHeader("auth-token");
            if(token !=null){
                UserProfile up = userTokenCache.getUserProfileFromToken(token);
                if(up!=null && up.active) {
                    List<String> privileges = privilegeService.getPrivilegesForRoles(up.getRoles());
                    auth = new LegacyUserTokenAuthentication(up, token, privileges);
                }
            }
        }
        if(auth ==null) {
            String username = request.getHeader("auth-username");
            String key = request.getHeader("auth-key");
            if (username != null && key != null) {
                UserProfile up =
                        Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(username))
                                .map(oo -> oo.standardize())
                                .orElse(null);
                if (up == null && authenticationConfiguration.isAutoregister()) {
                    up = autoregisterNewUser(username);

                }
                if (up != null && up.active) {
                    if (up.acceptKey(key)) {
                        //valid key!
                        List<String> privileges = privilegeService.getPrivilegesForRoles(up.getRoles());
                        auth = new LegacyUserKeyAuthentication(up, key, privileges);
                    } else {
                        throw new BadCredentialsException("invalid credentials for username: " + username);
                    }
                }
            }
        }
        //if we get here we don't have a valid login
        if(auth==null && !authenticationConfiguration.isAllownonauthenticated()) {
            response.setStatus(403);
            String message = "You are not authorized to see this resource. Please contact an administrator to be granted access.";

            try {
                response.setContentType("application/json");
                response.getWriter().println("{\"status\" : \"403\", \"message\" : \""+message+"\"}");
                return;
            } catch (IOException e) {
                throw new NonAuthenticatedUserAllowedException("not authorized to see this resource");
            }
        }
        if(auth !=null) {
            //add a new Session each time !?
            if (auth instanceof GsrsUserProfileDetails) {
                userTokenCache.updateUserCache(((GsrsUserProfileDetails)auth).getPrincipal());
            }

            //TODO: perhaps allow a short-circuit here if auth is outsourced
            request.getSession().setAttribute("username", auth.getName());
            // Check value to avoid error on UI login I think related to token
            // The error is "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead"           
            if(!auth.isAuthenticated()) {
                auth.setAuthenticated(true);
            }
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);

    }

    private UserProfile autoregisterNewUser(String username ) {
        List<Role> defaultRoles = Collections.singletonList(Role.of("Query"));
        return autoregisterNewUser(username, null, defaultRoles);
    }
    private UserProfile autoregisterNewUser(String username, String email, List<Role> roles ) {
        Principal p =  Principal.createStandardized(username, email);
        UserProfile up = new UserProfile(p);
        if (authenticationConfiguration.isAutoregisteractive()) {
            up.active = true;
        }
        up.systemAuth = false;
        if(roles !=null){
            up.setRoles(roles);
        }
        //should cascade new Principal
        repository.saveAndFlush(up);
        return up;
    }

    @Transactional
    private void saveUserProfile(UserProfile profile) {
       log.trace("saving up within transaction");
       repository.saveAndFlush(profile);
    }
}
