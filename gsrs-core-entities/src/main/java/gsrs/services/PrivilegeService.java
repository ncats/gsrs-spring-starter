package gsrs.services;

import gsrs.security.RoleConfiguration;
import gsrs.security.UserRoleConfiguration;
import gsrs.security.GsrsSecurityUtils;
import gsrs.security.UserRoleConfigurationLoader;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Slf4j
@Component("permission")
public class PrivilegeService {

    private static PrivilegeService instance = new PrivilegeService();
    private UserRoleConfiguration configuration;

    public static PrivilegeService instance() {
        return instance;
    }

    public PrivilegeService(){
        try {
            UserRoleConfigurationLoader loader = new UserRoleConfigurationLoader();
            this.configuration = loader.getConfiguration();
            log.trace("loaded configuration from file");
        } catch (IOException e) {
            log.error("Error loading configuration from file {}", e.getMessage(), e);
            log.warn("Will use default configuration");
            setDefaultConfig();
        }
    }

    private void setDefaultConfig() {
        //bootstrap some configuration
        configuration = new UserRoleConfiguration();
        List<RoleConfiguration> roles = new ArrayList<>();
        // we will read this from a file eventually
        RoleConfiguration queryRole = new RoleConfiguration();
        queryRole.setName("Query");
        //search and Query are synonymous
        String[] queryPrivileges ={"Login", CommonPrivileges.SEARCH, CommonPrivileges.BROWSE, CommonPrivileges.EXPORT_DATA };
        queryRole.setPrivileges(Arrays.asList(queryPrivileges));
        roles.add(queryRole);

        RoleConfiguration editorRole = new RoleConfiguration();
        editorRole.setName("DataEntry");
        String[] editorPrivileges ={CommonPrivileges.CREATE, CommonPrivileges.EDIT};
        editorRole.setPrivileges(Arrays.asList(editorPrivileges));
        editorRole.setInclude(Collections.singletonList("Query"));
        roles.add(editorRole);

        RoleConfiguration approverRole = new RoleConfiguration();
        approverRole.setName("Approver");
        String[] approverPrivileges = { CommonPrivileges.APPROVE_RECORDS, CommonPrivileges.EDIT_PUBLIC_DATA,
                CommonPrivileges.MODIFY_RELATIONSHIPS, CommonPrivileges.MAKE_RECORDS_PUBLIC, CommonPrivileges.EDIT_APPROVED_RECORDS };
        approverRole.setPrivileges(Arrays.asList(approverPrivileges));
        approverRole.setInclude(Collections.singletonList("DataEntry"));
        roles.add(approverRole);

        RoleConfiguration adminRole = new RoleConfiguration();
        adminRole.setName("Admin");
        String[] adminPrivileges = {CommonPrivileges.MANAGE_USERS, CommonPrivileges.CONFIGURE_SYSTEM, CommonPrivileges.MANAGE_VOCABULARIES, CommonPrivileges.IMPORT_DATA,
                CommonPrivileges.MERGE_SUBCONCEPTS, CommonPrivileges.MODIFY_RELATIONSHIPS, CommonPrivileges.EDIT_APPROVAL_IDS, CommonPrivileges.VIEW_SERVER_FILES,
                CommonPrivileges.VIEW_SERVICE_INFO};
        adminRole.setPrivileges(Arrays.asList(adminPrivileges));
        adminRole.setInclude(Collections.singletonList("Approver"));
        roles.add(adminRole);
        configuration.setRoles(roles);
    }

    public UserRoleConfiguration getConfiguration() {
        return this.configuration;
    }

    public boolean canDo(String thingToDo) {
        log.trace("in canDo function checking {}", thingToDo);
        boolean canDoResult =canUserPerform(thingToDo).equals(UserRoleConfiguration.PermissionResult.MayPerform);
        log.trace("canDo will return {}", canDoResult);
        return canDoResult;
    }

    public UserRoleConfiguration.PermissionResult canUserPerform(String task) {
        log.trace("in canUserPerform going to evaluate {}",task);
        if( GsrsSecurityUtils.getCurrentUser() instanceof UserProfile) {
            UserProfile currentUserProfile = (UserProfile)GsrsSecurityUtils.getCurrentUser();
            for(Role role: currentUserProfile.getRoles() ){
                log.trace(" canUserPerform looking at role {}", role.getRole());
                if( canRolePerform(role.getRole(), task) ){
                    log.trace("canUserPerform will return MayPerform");
                    return UserRoleConfiguration.PermissionResult.MayPerform;
                }
            }
            if(configuration.getRoles().stream()
                    .map(RoleConfiguration::getName)
                    .anyMatch(rn->currentUserProfile.getRoles().stream().anyMatch(ur->ur.getRole().equalsIgnoreCase(rn)))){
                return UserRoleConfiguration.PermissionResult.MayNotPerform;
            }
        } else if( GsrsSecurityUtils.getCurrentUser() instanceof User) {
            //expect this route on unit tests
            User currentUser = (User) GsrsSecurityUtils.getCurrentUser();
            return currentUser.getAuthorities().stream()
                    .filter(ga-> ga.getAuthority().startsWith("ROLE_"))
                    .map(ga->ga.getAuthority().substring(5))
                    .anyMatch(r-> canRolePerform(r, task))
                    ? UserRoleConfiguration.PermissionResult.MayPerform
                    : UserRoleConfiguration.PermissionResult.MayNotPerform;
        }
        return  UserRoleConfiguration.PermissionResult.RoleNotFound;
    }

    public boolean canRolePerform(String role, String task) {
        log.trace("in canRolePerform with role {} and task {}", role, task);
        for(RoleConfiguration configuredRole : this.configuration.getRoles()) {
            if( configuredRole.getName().equalsIgnoreCase(role)){
                log.trace("roles match; now look through it privs {}", configuredRole.getPrivileges());
                if( configuredRole.getPrivileges().stream().anyMatch( p-> p.equalsIgnoreCase(task))) {
                    log.trace("will return true!");
                    return true;
                }else if(configuredRole.getInclude()!=null) {
                    for( String includedRole : configuredRole.getInclude()){
                        if( canRolePerform(includedRole, task) ) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public List<String> getPrivilegesForConfiguredRole(String configuredRole) {
        Set<String> basePrivileges = new HashSet<>();
        RoleConfiguration matchingRole = configuration.getRoles().stream()
                .filter(r->r.getName().equalsIgnoreCase(configuredRole))
                .findFirst()
                .orElse(null);
        if(matchingRole != null) {
            basePrivileges.addAll(matchingRole.getPrivileges());
            for( String includedRole: matchingRole.getInclude()) {
                basePrivileges.addAll(getPrivilegesForConfiguredRole(includedRole));
            }
        }
        return new ArrayList<>(basePrivileges);
    }

    public List<String> getAllRoleNames() {
        return configuration.getRoles().stream().map(RoleConfiguration::getName).collect(Collectors.toList());
    }

    public List<String> getPrivilegesForRoles(List<Role> roles) {
        Set<String> privileges = new HashSet<>();
        roles.stream()
                .map(Role::getRole)
                .forEach(rn-> privileges.addAll(getPrivilegesForConfiguredRole(rn)));
        return new ArrayList<>(privileges);
    }

    public List<String> getAllUserPrivileges() {
        if( GsrsSecurityUtils.getCurrentUser() instanceof UserProfile) {
            UserProfile userProfile = (UserProfile) GsrsSecurityUtils.getCurrentUser();
            return getPrivilegesForRoles(userProfile.getRoles());
        }
        return Collections.emptyList();
    }
}
