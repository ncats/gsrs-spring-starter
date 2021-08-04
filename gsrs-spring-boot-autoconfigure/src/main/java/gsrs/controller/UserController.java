package gsrs.controller;

import gsrs.repository.GroupRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.security.hasAdminRole;
import gsrs.services.GroupService;
import gsrs.services.UserProfileService;
import ix.core.models.Group;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@hasAdminRole
@RestController
public class UserController {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PrincipalRepository principalRepository;
    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;


    @GetMapping("api/v1/users")
    @Transactional(readOnly = true)
    public ResponseEntity<Object> users(@RequestParam(value = "top", defaultValue = "10") long top,
                                        @RequestParam(value = "skip", defaultValue = "0") long skip,
                                        @RequestParam Map<String, String> queryParameters) {


        Page<UserProfile> users = userProfileRepository.findAll(new OffsetBasedPageRequest(skip, top));
        String view = queryParameters.get("view");
        if ("key".equals(view)) {
            return new ResponseEntity<>(AbstractGsrsEntityController.PagedResult.ofKeys(users), HttpStatus.OK);

        }
        return new ResponseEntity<>(new AbstractGsrsEntityController.PagedResult(users, queryParameters), HttpStatus.OK);
    }

    @GetMapping({"api/v1/users({ID})", "api/v1/users/{ID}"})
    public ResponseEntity<Object> getSingleRecord(@RequestParam("ID") String idOrName, @RequestParam Map<String, String> queryParameters) {
        Optional<UserProfile> opt = getUserProfile(idOrName);
        if (opt.isPresent()) {
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(opt.get(), queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    /**
     * We never actually remove a User from the database,
     * we just set them to not active.
     *
     * @param idOrName        the id or username of the user to modify.
     * @param queryParameters any url query parameters which might change how the result or http status code is returned.
     * @return the enhanced UserProfile object.
     */
    @Transactional
    @DeleteMapping({"api/v1/users({ID})", "api/v1/users/{ID}"})
    public ResponseEntity<Object> deactivateUser(@PathVariable("ID") String idOrName, @RequestParam Map<String, String> queryParameters) {
        Optional<UserProfile> opt = getUserProfile(idOrName);
        if (opt.isPresent()) {
            UserProfile up = opt.get();
            //TODO move this to UserProfile service?
            up.deactivate();
            userProfileRepository.saveAndFlush(up);
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(up, queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    private Optional<UserProfile> getUserProfile(String idOrName) {
        Optional<UserProfile> opt = Optional.empty();
        if (IdHelpers.NUMBER.getPattern().matcher(idOrName).matches()) {
            //is an id
            long idNum = Long.parseLong(idOrName);
            opt = userProfileRepository.findById(idNum);

        }
        if (!opt.isPresent()) {
            //if we're here look for a username?
            opt = Optional.ofNullable(userProfileRepository.findByUser_Username(idOrName));
        }
        return opt;
    }

    @Transactional
    @PostMapping({"api/v1/users({ID})/password", "api/v1/users/{ID}/password"})
    public ResponseEntity<Object> changePassword(@PathVariable("ID") String idOrName,
                                                 @RequestBody PasswordChangeRequest passwordChangeRequest,
                                                 @RequestParam Map<String, String> queryParameters) {
        Optional<UserProfile> opt = getUserProfile(idOrName);
        if (opt.isPresent()) {
            UserProfile up = opt.get();
            //as admins we don't need to check if the old password matches we can force update the password
            up.setPassword(passwordChangeRequest.getNewPassword());
            userProfileRepository.saveAndFlush(up);
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(up, queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @Data
    public static class PasswordChangeRequest {
        private String oldPassword;
        private String newPassword;


    }

    /*
    POST    /users/                 ix.core.controllers.v1.UserController.createNewUser()
PUT     /users/:username        ix.core.controllers.v1.UserController.updateUserByUsername(username: String)
PUT     /users($username<[0-9]+>)        ix.core.controllers.v1.UserController.updateUserById(username: Long)
     */

    @Transactional
    @PostMapping({"api/v1/users"})
    public ResponseEntity<Object> createNewUserProfile(
            @RequestBody UserProfileService.NewUserRequest newUserRequest,
            @RequestParam Map<String, String> queryParameters) {


        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(
                userProfileService.createNewUserProfile(newUserRequest.createValidatedNewUserRequest()), queryParameters), HttpStatus.OK);
    }
    @Transactional
    @PutMapping({"api/v1/users({ID})", "api/v1/users/{ID}"})
    public ResponseEntity<Object> updateUserProfile(
            @PathVariable("ID") String idOrName,
            @RequestBody UserProfileService.NewUserRequest newUserRequest,
            @RequestParam Map<String, String> queryParameters) {

        Optional<UserProfile> opt = getUserProfile(idOrName);
        if (!opt.isPresent()) {
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        UserProfileService.ValidatedNewUserRequest validatedNewUserRequest = newUserRequest.createValidatedNewUserRequest();
        String requestedUsername = validatedNewUserRequest.getUsername();
        //make sure it matches
        if(!opt.get().user.username.equals(requestedUsername)){
            return gsrsControllerConfiguration.handleBadRequest(400, "username doesn't match", queryParameters);
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(
                userProfileService.updateUserProfile(validatedNewUserRequest), queryParameters), HttpStatus.OK);
    }
}


