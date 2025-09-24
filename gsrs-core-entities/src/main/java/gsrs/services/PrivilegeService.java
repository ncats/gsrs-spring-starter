package gsrs.services;

import gsrs.security.RoleConfiguration;
import gsrs.security.UserRoleConfiguration;
import gsrs.security.GsrsSecurityUtils;
import gsrs.security.UserRoleConfigurationLoader;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Slf4j
@Component("permission")
public class PrivilegeService {

    private UserRoleConfiguration configuration;

    public PrivilegeService(){
        try {
            UserRoleConfigurationLoader loader = new UserRoleConfigurationLoader();
            this.configuration = loader.getConfiguration();
            log.trace("loaded configuration from file");
        } catch (IOException e) {
            log.error("Error loading configuration from file {}", e.getMessage());
            e.printStackTrace();
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
        String[] queryPrivileges ={"Login", "Search", "Browse", "Export" };
        queryRole.setPrivileges(Arrays.asList(queryPrivileges));
        roles.add(queryRole);

        RoleConfiguration editorRole = new RoleConfiguration();
        editorRole.setName("DataEntry");
        //Edit and Change are synonymous
        String[] editorPrivileges ={"Create", "Edit"};
        editorRole.setPrivileges(Arrays.asList(editorPrivileges));
        editorRole.setInclude(Collections.singletonList("Query"));
        roles.add(editorRole);

        RoleConfiguration approverRole = new RoleConfiguration();
        approverRole.setName("Approver");
        String[] approverPrivileges = {"Approve Records", "Edit Public Data"};
        approverRole.setPrivileges(Arrays.asList(approverPrivileges));
        approverRole.setInclude(Collections.singletonList("DataEntry"));
        roles.add(approverRole);

        RoleConfiguration adminRole = new RoleConfiguration();
        adminRole.setName("Admin");
        String[] adminPrivileges = {"Manage Users", "Manage Vocabularies", "Configure System", "Manage CVs", "Import Data"};
        adminRole.setPrivileges(Arrays.asList(adminPrivileges));
        adminRole.setInclude(Collections.singletonList("Approver"));
        roles.add(adminRole);
        configuration.setRoles(roles);
    }

    public UserRoleConfiguration getConfiguration() {
        return this.configuration;
    }

    public boolean canDo(String thingToDo) {
        log.trace("in canDo function checking{}", thingToDo);
        /*if( GsrsSecurityUtils.getCurrentUser() instanceof UserProfile) {
            UserProfile currentUserProfile = (UserProfile) GsrsSecurityUtils.getCurrentUser();
            currentUserProfile.properties.forEach(p->p.id);
        }*/
        boolean canDoResult =canUserPerform(thingToDo).equals(UserRoleConfiguration.PermissionResult.MayPerform);
        log.trace("canDo will return {}", canDoResult);
        return canDoResult;
    }

    public UserRoleConfiguration.PermissionResult canUserPerform(String task) {
        log.trace("in canUserPerform going to evaluate {}",task);
        if( GsrsSecurityUtils.getCurrentUser() instanceof UserProfile) {
            UserProfile currentUserProfile = (UserProfile)GsrsSecurityUtils.getCurrentUser();
            for(Role role: currentUserProfile.getRoles() ){
                log.trace(" canUserPerform looking at role {}", role.name());
                if( canRolePerform(role.name(), task) ){
                    log.trace("canUserPerform will return MayPerform");
                    return UserRoleConfiguration.PermissionResult.MayPerform;
                }
            }
            if(configuration.getRoles().stream()
                    .map(r->r.getName())
                    .anyMatch(rn->currentUserProfile.getRoles().stream().anyMatch(ur->ur.name().equalsIgnoreCase(rn)))){
                return UserRoleConfiguration.PermissionResult.MayNotPerform;
            }
        }
        return  UserRoleConfiguration.PermissionResult.RoleNotFound;
    }

    private boolean canRolePerform(String role, String task) {
        log.trace("in canRolePerform with role {} and task {}", role, task);
        for(RoleConfiguration configuredRole : this.configuration.getRoles()) {
            //log.trace("comparing configured role {} to user role {}", configuredRole.getName(), role);
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
        return basePrivileges.stream().collect(Collectors.toList());
    }

    public List<String> getPrivilegesForRoles(List<Role> roles) {
        Set<String> privileges = new HashSet<>();
        roles.stream()
                .map(r->r.name())
                .forEach(rn-> privileges.addAll(getPrivilegesForConfiguredRole(rn)));
        return privileges.stream().collect(Collectors.toList());
    }
}
