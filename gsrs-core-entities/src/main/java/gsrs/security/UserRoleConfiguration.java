package gsrs.security;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class UserRoleConfiguration {

    public enum PermissionResult {
        RoleNotFound,
        MayPerform,
        MayNotPerform
    }
    private List<RoleConfiguration> roles = Collections.emptyList();
}
