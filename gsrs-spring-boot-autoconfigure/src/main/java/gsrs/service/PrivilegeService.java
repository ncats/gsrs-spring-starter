package gsrs.service;

import gsrs.autoconfigure.RoleConfiguration;
import gsrs.autoconfigure.UserRoleConfiguration;
import gsrs.security.GsrsSecurityUtils;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Slf4j
public class PrivilegeService {
    private UserRoleConfiguration configuration;

    public PrivilegeService(){
        //bootstrap some configuration
        configuration = new UserRoleConfiguration();
        List<RoleConfiguration> roles = new ArrayList<>();
        // we will read this from a file eventually
        RoleConfiguration queryRole = new RoleConfiguration();
        queryRole.setName("Query");
        //search and Query are synonymous
        String[] queryPrivileges ={"Login", "Search", "Browse", "Export", "Query" };
        queryRole.setPrivileges(Arrays.asList(queryPrivileges));
        roles.add(queryRole);

        RoleConfiguration editorRole = new RoleConfiguration();
        editorRole.setName("DataEntry");
        //Edit and Change are synonymous
        String[] editorPrivileges ={"Create", "Edit", "Change"};
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
        String[] adminPrivileges = {"Manage Users", "Manage Vocabularies", "Configure System", "Manage CVs"};
        adminRole.setPrivileges(Arrays.asList(adminPrivileges));
        adminRole.setInclude(Collections.singletonList("Approver"));
        roles.add(adminRole);

        configuration.setRoles(roles);
    }

    public UserRoleConfiguration.PermissionResult canUserPerform(String task) {
        if( GsrsSecurityUtils.getCurrentUser() instanceof UserProfile) {
            UserProfile currentUserProfile = (UserProfile)GsrsSecurityUtils.getCurrentUser();
            for(Role role: currentUserProfile.getRoles() ){
                if( canRolePerform(role.name(), task) ){
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
        for(RoleConfiguration configuredRole : this.configuration.getRoles()) {
            log.trace("comparing configured role {} to user role {}", configuredRole.getName(), role);
            if( configuredRole.getName().equalsIgnoreCase(role)){
                log.trace("roles match; now look through it privs");
                if( configuredRole.getPrivileges().stream().anyMatch( p-> p.equalsIgnoreCase(task))) {
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
}
