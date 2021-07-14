package gsrs.controller;

import gsrs.cache.GsrsCache;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.EntityResponse;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
public class LoginController {
    @Autowired
    private UserProfileRepository repository;
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
        // Add a session cookie
        UUID sessionId = session.get().id;
        Cookie sessionCookie = new Cookie( sessionCookieName, sessionId.toString());
        sessionCookie.setHttpOnly(true);
        response.addCookie( sessionCookie );
        gsrsCache.setRaw(sessionId.toString(), sessionId);
        return new ResponseEntity<>(up, HttpStatus.OK);
    }
}
