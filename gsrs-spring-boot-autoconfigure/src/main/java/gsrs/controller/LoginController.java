package gsrs.controller;

import gsrs.cache.GsrsCache;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.repository.GroupRepository;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Group;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.function.EntityResponse;

import gov.nih.ncats.common.util.TimeUtil;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class LoginController {
    @Autowired
    private UserProfileRepository repository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private SessionRepository sessionRepository;


    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private GsrsCache gsrsCache;

    //TODO this is the default session cookie name Spring uses or should we just use ix.session
    @Value("${gsrs.sessionKey}")
    private String sessionCookieName;

    @Value("#{new Long('${gsrs.sessionExpirationMS:-1}')}")
    private Long sessionExpirationMS;

    @Value("#{new Boolean('${gsrs.sessionSecure:true}')}")
    private Boolean sessionCookieSecure;

    //dkatzel: we turned off "isAuthenticated()" so we can catch the access is denied error
    //so we can customize it. but that didn't work as the Session info assumes authentication
    //has already run and registered your session

    // This method is called:
    // after credentials are checked.
    // when a cookie session key is checked successfully.

    @PreAuthorize("isAuthenticated()")
    @GetMapping("api/v1/whoami")
//    @Transactional(readOnly = true)
    @Transactional
    public ResponseEntity<Object> login(Principal principal, @RequestParam Map<String, String> parameters,
                                        HttpServletResponse response){

        System.out.println("LOGIN controller login __alex__");

        UserProfile up =null;
        if(principal !=null){
            up = Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(principal.getName()))
                         .map(u->u.standardize())
                         .orElse(null);
        }
        if(up ==null){
            return gsrsControllerConfiguration.handleNotFound(parameters);
        }

        Optional<Session> session = this.cleanUpSessionsThenGetSession(up);

        /*
        // Should we not have a Utility method called something like
        // Session s = cleanUpSessions(UserProfile up, List<Sessions> sessions)
        // that handles this in all places?
        // Also, is not being done twice when user provides credentials?
//
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

        //copied this from LoginAndLogoutEventListener
        if(sessions.isEmpty()){
            //make new one?
            Session s = new Session();
            //this is so we don't have a stale entity
            s.profile = Optional.ofNullable(up)
                    .map(oo->oo.standardize())
                    .orElse(null);
            sessionRepository.saveAndFlush(s);
        }
         else{
            // maybe this is not necessary? when just checking valid session?
            long time = System.currentTimeMillis();
            for(Session s : sessions){
                s.accessed = time;
            }
            sessionRepository.saveAll(sessions);
        }

        Optional<Session> session = sessions.stream().findFirst();
*/

        UUID sessionId = session.get().id;
        Cookie sessionCookie = new Cookie( sessionCookieName, sessionId.toString());
        sessionCookie.setHttpOnly(true);
        if(sessionCookieSecure ==null || sessionCookieSecure.booleanValue()){
            sessionCookie.setSecure(true);
        }
        sessionCookie.setPath("/"); //Maybe?
        
        
        response.addCookie( sessionCookie );
        
        
//        System.out.println("set cookie:" + sessionId);
        
//        gsrsCache.setRaw(sessionId.toString(), sessionId);
        //we actually want to include the computed token here
        //which we jsonignore otherwise

        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(up,parameters, m->{
            UserProfile userProfile = (UserProfile)m.getObj();
            m.addKeyValuePair("computedToken", userProfile.getComputedToken());
            List<Group> groups = groupRepository.findGroupsByMembers(userProfile.user);
            m.addKeyValuePair("groups", groups==null?Collections.emptyList(): groups);
        }), HttpStatus.OK);
    }
    //this is a GET in 2.x keep it for backwards compatibility
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @GetMapping({"api/v1/profile/@keygen"})
    public Object regenerateMyKey(Principal principal,
                                  @RequestParam Map<String, String> queryParameters){
        Optional<UserProfile> opt = Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(principal.getName()));
        if (opt.isPresent()) {
            UserProfile up = opt.get();
            up.regenerateKey();
            repository.saveAndFlush(up);
            return new ResponseEntity<>(enhanceUserProfile(up, queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    @PostMapping({"api/v1/profile/@keygen"})
    public Object regenerateMyKeyPost(Principal principal,
                                  @RequestParam Map<String, String> queryParameters){
        Optional<UserProfile> opt = Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(principal.getName()));
        if (opt.isPresent()) {
            UserProfile up = opt.get();
            up.regenerateKey();
            repository.saveAndFlush(up);
            return new ResponseEntity<>(enhanceUserProfile(up, queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @PostMapping({"api/v1/profile/password"})
    public ResponseEntity<Object> changePassword(Principal principal,
                                                 @RequestBody UserController.PasswordChangeRequest passwordChangeRequest,
                                                 @RequestParam Map<String, String> queryParameters) {
        Optional<UserProfile> opt = Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(principal.getName()));
        if (opt.isPresent()) {
            UserProfile up = opt.get();
            //TODO implement better rules or configable rules for passwords?
            if(passwordChangeRequest.getNewPassword() ==null || passwordChangeRequest.getNewPassword().trim().isEmpty()){
                return gsrsControllerConfiguration.handleBadRequest(400,"password can not be blank or all whitespace", queryParameters);
            }
            //check old password
            if(!up.acceptPassword(passwordChangeRequest.getOldPassword())){
                return gsrsControllerConfiguration.unauthorized("incorrect password", queryParameters);
            }
            up.setPassword(passwordChangeRequest.getNewPassword());
            repository.saveAndFlush(up);
            return new ResponseEntity<>(enhanceUserProfile(up, queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    private GsrsUnwrappedEntityModel enhanceUserProfile(UserProfile up, Map<String, String> queryParameters){
        return GsrsControllerUtil.enhanceWithView(up, queryParameters, m->{
            UserProfile profile= ((UserProfile)m.getObj());
            List<Group> groups = groupRepository.findGroupsByMembers(profile.user);

            m.addKeyValuePair("groups", groups==null?Collections.emptyList(): groups);

        });
    }

    Optional<Session> cleanUpSessionsThenGetSession(UserProfile up) {
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

        //copied this from LoginAndLogoutEventListener
        if(sessions.isEmpty()){
            //make new one?
            Session s = new Session();
            //this is so we don't have a stale entity
            s.profile = Optional.ofNullable(up)
                    .map(oo->oo.standardize())
                    .orElse(null);
            sessionRepository.saveAndFlush(s);
        }
        else{
            // maybe this is not necessary? when just checking valid session?
            long time = System.currentTimeMillis();
            for(Session s : sessions){
                s.accessed = time;
            }
            sessionRepository.saveAll(sessions);
        }
        Optional<Session> session = sessions.stream().findFirst();
        return session;
    }
}
