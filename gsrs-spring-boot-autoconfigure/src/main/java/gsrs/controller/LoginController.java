package gsrs.controller;

import gsrs.cache.GsrsCache;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.model.UserProfileAuthenticationResult;
import gsrs.repository.GroupRepository;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.security.SessionConfiguration;
import ix.core.models.Group;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.*;

@RestController
public class LoginController {
    @Autowired
    private UserProfileRepository repository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionConfiguration sessionConfiguration;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private GsrsCache gsrsCache;

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
        UserProfile up =null;
        if(principal !=null){
            up = Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(principal.getName()))
                    .map(u->u.standardize())
                    .orElse(null);
        }
        if(up ==null){
            return gsrsControllerConfiguration.handleNotFound(parameters);
        }

        Optional<Session> session = sessionConfiguration.cleanUpSessionsThenGetSession(up);

        UUID sessionId = session.get().id;
        Cookie sessionCookie = new Cookie( sessionConfiguration.getSessionCookieName(), sessionId.toString());
        sessionCookie.setHttpOnly(true);
        if(sessionConfiguration.getSessionCookieSecure() ==null || sessionConfiguration.getSessionCookieSecure().booleanValue()){
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
            UserProfileAuthenticationResult authenticationResult =up.acceptPassword(passwordChangeRequest.getOldPassword());
            if(!authenticationResult.matchesRepository()){
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
}
