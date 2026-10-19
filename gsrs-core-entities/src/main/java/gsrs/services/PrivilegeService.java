package gsrs.services;

import gsrs.security.RoleConfiguration;
import gsrs.security.UserRoleConfiguration;
import gsrs.security.GsrsSecurityUtils;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Data
@Slf4j
@Component("permission")
public class PrivilegeService {

    @Autowired
    @Lazy
    private UserRoleConfiguration configuration;

    private List<RoleConfiguration> configuredRoles;

    private static class Holder {
        static final PrivilegeService INSTANCE = new PrivilegeService();
    }

    public static PrivilegeService instance() {
        return Holder.INSTANCE;
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
        checkRoles();
        if( GsrsSecurityUtils.getCurrentUser() instanceof UserProfile) {
            UserProfile currentUserProfile = (UserProfile)GsrsSecurityUtils.getCurrentUser();
            for(Role role: currentUserProfile.getRoles() ){
                log.trace(" canUserPerform looking at role {}", role.getRole());
                if( canRolePerform(role.getRole(), task) ){
                    log.trace("canUserPerform will return MayPerform");
                    return UserRoleConfiguration.PermissionResult.MayPerform;
                }
            }
            if(configuredRoles.stream()
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

    public boolean  canRolePerform(String role, String task) {
        log.trace("in canRolePerform with role {} and task {}", role, task);
        checkRoles();
        for(RoleConfiguration configuredRole : configuredRoles) {
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
        checkRoles();
        Set<String> basePrivileges = new HashSet<>();
        RoleConfiguration matchingRole = configuredRoles.stream()
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

    private void checkRoles() {
        if( configuredRoles == null || configuredRoles.isEmpty()) {
            if( configuration == null) {
                AutowireHelper.getInstance().autowireAndProxy(this);
            }
            if( configuration != null && configuration.getRoles() != null) {
                configuredRoles = new ArrayList<>();
                configuration.getRoles().forEach((name, roleConfig) -> {
                    // the role name is the map key; propagate it onto the value
                    roleConfig.setName(name);
                    configuredRoles.add(roleConfig);
                });
                log.trace("loaded configuration from properties. total: {}", configuredRoles.size());
            }
        }
    }

    /**
     * @return the configured roles keyed by role name, or an empty map if none are configured.
     */
    public Map<String, RoleConfiguration> getConfiguredRolesByName() {
        checkRoles();
        Map<String, RoleConfiguration> byName = new LinkedHashMap<>();
        configuredRoles.forEach(r -> byName.put(r.getName(), r));
        return byName;
    }
    public List<String> getAllRoleNames() {
        checkRoles();
        return configuredRoles.stream().map(RoleConfiguration::getName).collect(Collectors.toList());
    }

    public List<String> getPrivilegesForRoles(List<Role> roles) {
        checkRoles();
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
