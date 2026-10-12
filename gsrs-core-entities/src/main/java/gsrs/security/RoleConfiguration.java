package gsrs.security;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class RoleConfiguration {
    /**
     * The role name. This is populated from the {@code roles} map key in
     * {@code UserRoleConfiguration} (see {@code PrivilegeService#checkRoles()})
     * and is not configured directly.
     */
    private String name;
    private List<String> privileges = Collections.emptyList();
    private List<String> include= Collections.emptyList();
}
