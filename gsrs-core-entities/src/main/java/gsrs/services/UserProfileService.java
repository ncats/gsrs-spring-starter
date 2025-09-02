package gsrs.services;

import gsrs.repository.GroupRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.security.hasAdminRole;
import gsrs.springUtils.GsrsSpringUtils;
import ix.core.models.Group;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;



    private final GroupService groupService;

    private GroupRepository groupRepository;

    private EntityManager entityManager;

    @Autowired
    public UserProfileService(UserProfileRepository userProfileRepository,
                              GroupService groupService,
                              GroupRepository groupRepository,
                              EntityManager entityManager
                              ) {
        this.userProfileRepository = userProfileRepository;
        this.groupService = groupService;
        this.groupRepository = groupRepository;
        this.entityManager = entityManager;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewUserRequest{
        private String username;
        private String password;
        private String email;
        private Boolean isAdmin;
        private Boolean isActive;
        private Set<String> groups;
        private Set<String> roles;


        private static Pattern WHITESPACE_PATERN = Pattern.compile("\\s+");

        public ValidatedNewUserRequest createValidatedNewUserRequest(){
            return new ValidatedNewUserRequest(validateUserName(username.trim()), nullSafeTrim(password),
                    nullSafeTrim(email), isAdmin, isActive,
                    trimAndRemoveNulls(groups), convertToRoles(roles)
                                                );
        }
        private String validateUserName(String name){
            Objects.requireNonNull(name);
            if(WHITESPACE_PATERN.matcher(name).find()){
                throw new IllegalArgumentException("can not contain whitespace");
            }
            return name;
        }
        private String nullSafeTrim(String input){
            if(input ==null){
                return null;
            }
            return input.trim();
        }
        private Set<String> trimAndRemoveNulls(Set<String> input){
            if(input==null){
                return null;
            }
            return input.stream().map(s-> s ==null?null: s.trim()).filter(Objects::nonNull).collect(Collectors.toSet());
        }
        private Set<Role> convertToRoles(Set<String> input){
            if(input==null){
                return null;
            }
            return input.stream().map(s-> s ==null?null: s.trim()).filter(Objects::nonNull)
                    .map(Role::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
        }
    }


    public static class ValidatedNewUserRequest{
        private final String username;
        private final String password;
        private final String email;
        private final Boolean isAdmin;
        private final Boolean isActive;
        private final Set<String> groups;
        private final Set<Role> roles;

        private ValidatedNewUserRequest(String username, String password, String email, Boolean isAdmin, Boolean isActive, Set<String> groups, Set<Role> roles) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.isAdmin = isAdmin;
            this.isActive = isActive;
            this.groups = groups;
            this.roles = roles;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getEmail() {
            return email;
        }

        public Boolean isAdmin() {
            return isAdmin;
        }

        public Boolean isActive() {
            return isActive;
        }

        public Set<String> getGroups() {
            return groups;
        }

        public Set<Role> getRoles() {
            return roles;
        }

    }
    @Transactional
    @hasAdminRole
    public UserProfile updateUserProfile(ValidatedNewUserRequest newUserRequest) {

        synchronized (this) {
            /*


        private final Set<String> groups;
        private final Set<Role> roles;
             */
            UserProfile oldUser = Optional.ofNullable(userProfileRepository.findByUser_UsernameIgnoreCase(newUserRequest.getUsername()))
                    .map(oo->oo.standardize())
                    .orElse(null);
            if (oldUser == null) {
                throw new IllegalArgumentException("The username \"" + newUserRequest.getUsername() + "\" not found");

            }
            if(newUserRequest.getEmail() !=null){
                oldUser.user.email= newUserRequest.getEmail();
            }
            if(newUserRequest.getPassword() !=null){
                oldUser.setPassword(newUserRequest.getPassword());
            }
            if(newUserRequest.isActive() !=null){
                oldUser.active=newUserRequest.isActive();
            }
            if(newUserRequest.isAdmin() !=null){
                oldUser.user.admin=newUserRequest.isAdmin();
            }

            //or is hibernate smart enough to figure that out?
            //oh we need null vs empty in case we remove all groups?
            if(newUserRequest.getGroups() !=null){

                groupService.updateUsersGroups(oldUser.user, newUserRequest.getGroups());
            }
            if(newUserRequest.getRoles() !=null){
                oldUser.setRoles(newUserRequest.getRoles());
            }
            return oldUser;
        }
    }
    @hasAdminRole
    @Transactional
    public UserProfile createNewUserProfile(ValidatedNewUserRequest newUserRequest){
        Principal principal = new Principal();

        synchronized(this) {
            UserProfile oldUser = userProfileRepository.findByUser_UsernameIgnoreCase(newUserRequest.getUsername());
            if(oldUser !=null){
                throw new IllegalArgumentException("The username \"" + newUserRequest.getUsername() + "\" already exists");
            }
            principal.username = newUserRequest.getUsername();
            principal.email = newUserRequest.getEmail();
            principal.admin = Boolean.TRUE.equals(newUserRequest.isAdmin());
            principal.standardizeAndUpdate();

            UserProfile up = new UserProfile();
            up.user = principal;
            up.active = Boolean.TRUE.equals(newUserRequest.isActive());
            if (newUserRequest.getPassword() != null) {
                up.setPassword(newUserRequest.getPassword());
            }

            up.setRoles(newUserRequest.getRoles());

            //groups may not exists
            Set<String> groups = newUserRequest.getGroups();

            Set<Group> groupsToSave = new HashSet<>();
            if (groups != null) {
                for (String g : groups) {
                    Group group = groupService.registerIfAbsent(g);
                    group.addMember(principal);
                    groupsToSave.add(group);
//                    groupRepository.save(group);
                }
            }
            //TODO do we need to save user too? or will it cascade?
            UserProfile saved= userProfileRepository.saveAndFlush(up);
            if(!groupsToSave.isEmpty()) {
                groupRepository.saveAll(groupsToSave);
            }
            return saved;
        }

    }
}
