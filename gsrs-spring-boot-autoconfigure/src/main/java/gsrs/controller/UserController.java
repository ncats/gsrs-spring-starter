package gsrs.controller;

import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.repository.GroupRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.security.canManageUsers;
import gsrs.services.GroupService;
import gsrs.services.UserProfileService;
import ix.core.models.Group;
import ix.core.models.UserProfile;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@canManageUsers
@RestController
@Slf4j
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

    @GetMapping("api/v1/admin/groups/@names")
    @Transactional(readOnly = true)
    public List<String> getGroupNames(){
        return groupRepository.findAllGroupNames();
    }
    @GetMapping("api/v1/users")
    @Transactional(readOnly = true)
    public List<UserProfile> userSummary(
                                        @RequestParam Map<String, String> queryParameters) {
        //only need user, email created modified active/inactive
        //this selected/fetched query is to only fetch the columns we need in the admin panel api
        //the Summary nested objects are to preserve the same json structure as if we returned the real
        //objects with empty everything else we don't read.

        return userProfileRepository.findAll();

    }

    @GetMapping({"api/v1/users({ID})", "api/v1/users/{ID}"})
    public ResponseEntity<Object> getSingleRecord(@PathVariable("ID") String idOrName, @RequestParam Map<String, String> queryParameters) {
        Optional<UserProfile> opt = getUserProfile(idOrName);
        if (opt.isPresent()) {
            return new ResponseEntity<>(enhanceUserProfile(opt.get(), queryParameters), HttpStatus.OK);
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
    @canManageUsers
    public ResponseEntity<Object> deactivateUser(@PathVariable("ID") String idOrName, @RequestParam Map<String, String> queryParameters) {
        Optional<UserProfile> opt = getUserProfile(idOrName);
        if (opt.isPresent()) {
            UserProfile up = opt.get();
            //TODO move this to UserProfile service?
            up.deactivate();
            userProfileRepository.saveAndFlush(up);
            return new ResponseEntity<>(enhanceUserProfile(up, queryParameters), HttpStatus.OK);
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
            opt = Optional.ofNullable(userProfileRepository.findByUser_UsernameIgnoreCase(idOrName)).map(up->up.standardize());
        }
        return opt;
    }

    @Transactional
    @PostMapping({"api/v1/users({ID})/password", "api/v1/users/{ID}/password"})
    @canManageUsers
    public ResponseEntity<Object> changePassword(@PathVariable("ID") String idOrName,
                                                 @RequestBody PasswordChangeRequest passwordChangeRequest,
                                                 @RequestParam Map<String, String> queryParameters) {
        Optional<UserProfile> opt = getUserProfile(idOrName);
        if (opt.isPresent()) {
            UserProfile up = opt.get();
            //as admins we don't need to check if the old password matches we can force update the password
            up.setPassword(passwordChangeRequest.getNewPassword());
            userProfileRepository.saveAndFlush(up);
            return new ResponseEntity<>(enhanceUserProfile(up, queryParameters), HttpStatus.OK);
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
    @canManageUsers
    public ResponseEntity<Object> createNewUserProfile(
            @RequestBody UserProfileService.NewUserRequest newUserRequest,
            @RequestParam Map<String, String> queryParameters) {


        return new ResponseEntity<>(enhanceUserProfile(
                userProfileService.createNewUserProfile(newUserRequest.createValidatedNewUserRequest()), queryParameters), HttpStatus.OK);
    }
    @Transactional
    @PutMapping({"api/v1/users({ID})", "api/v1/users/{ID}"})
    @canManageUsers
    public ResponseEntity<Object> updateUserProfile(
            @PathVariable("ID") String idOrName,
            @RequestBody UserProfileService.NewUserRequest newUserRequest,
            @RequestParam Map<String, String> queryParameters) {

        log.trace("starting updateUserProfile");
        Optional<UserProfile> opt = getUserProfile(idOrName);
        if (!opt.isPresent()) {
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        log.trace("updateUserProfile retrieved UP");
        UserProfileService.ValidatedNewUserRequest validatedNewUserRequest = newUserRequest.createValidatedNewUserRequest();
        String requestedUsername = validatedNewUserRequest.getUsername();
        //make sure it matches
        if(!opt.get().user.username.equals(requestedUsername)){
            return gsrsControllerConfiguration.handleBadRequest(400, "username doesn't match", queryParameters);
        }
        return new ResponseEntity<>(enhanceUserProfile(
                userProfileService.updateUserProfile(validatedNewUserRequest), queryParameters), HttpStatus.OK);
    }

    private GsrsUnwrappedEntityModel enhanceUserProfile(UserProfile up, Map<String, String> queryParameters){
        return GsrsControllerUtil.enhanceWithView(up, queryParameters, m->{
            UserProfile profile= ((UserProfile)m.getObj());
            List<Group> groups = groupRepository.findGroupsByMembers(profile.user);

                m.addKeyValuePair("groups", groups==null?Collections.emptyList(): groups);

        });
    }
}


