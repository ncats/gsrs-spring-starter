package gsrs.controller;

import gsrs.cache.GsrsCache;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.EntityResponse;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
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
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private GsrsCache gsrsCache;

    //TODO this is the default session cookie name Spring uses or should we just use ix.session
    @Value("${gsrs.sessionKey}")
    private String sessionCookieName;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("api/v1/whoami")
    @Transactional(readOnly = true)
    public ResponseEntity<Object> login(Principal principal, @RequestParam Map<String, String> parameters,
                                        HttpServletResponse response){

        UserProfile up =null;
        if(principal !=null){
            up = repository.findByUser_Username(principal.getName());
        }
        if(up ==null){
            return gsrsControllerConfiguration.handleNotFound(parameters);
        }

        List<Session> sessions = sessionRepository.getActiveSessionsFor(up);
        Optional<Session> session = sessions.stream().findFirst();

        //session could be empty if we restarted or erased sessions on the server and a browser kept old sesion info
        // Add a session cookie

        UUID sessionId = session.get().id;
        Cookie sessionCookie = new Cookie( sessionCookieName, sessionId.toString());
        sessionCookie.setHttpOnly(true);
        sessionCookie.setPath("/"); //Maybe?
        
        response.addCookie( sessionCookie );
        gsrsCache.setRaw(sessionId.toString(), sessionId);
        //we actually want to include the computed token here
        //which we jsonignore otherwise

        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(up,parameters, m->{
            UserProfile userProfile = (UserProfile)m.getObj();
            m.addKeyValuePair("computedToken", userProfile.getComputedToken());
            List<Group> groups = groupRepository.findGroupsByMembers(userProfile.user);
            m.addKeyValuePair("groups", groups==null?Collections.emptyList(): groups);
        }), HttpStatus.OK);
    }
}
