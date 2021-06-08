package gsrs.controller;

import gsrs.repository.UserProfileRepository;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.EntityResponse;

import java.security.Principal;
import java.util.Map;

@RestController
public class LoginController {
    @Autowired
    private UserProfileRepository repository;
    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("api/v1/whoami")
    public ResponseEntity<Object> login(Principal principal, @RequestParam Map<String, String> parameters){

        UserProfile up =null;
        if(principal !=null){
            up = repository.findByUser_Username(principal.getName());
        }
        if(up ==null){
            return gsrsControllerConfiguration.handleNotFound(parameters);
        }
        return new ResponseEntity<>(up, HttpStatus.OK);
    }
}
