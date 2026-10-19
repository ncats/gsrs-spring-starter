package gsrs.security;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties("gsrs.security.info")
@Data
public class UserRoleConfiguration {

    private static  CachedSupplier<UserRoleConfiguration> _instanceSupplier =CachedSupplier.of(() ->{
        UserRoleConfiguration instance = StaticContextAccessor.getBean(UserRoleConfiguration.class);
        return instance;
    });
    public static UserRoleConfiguration getInstance() {
        return _instanceSupplier.get();
    }
    public enum PermissionResult {
        RoleNotFound,
        MayPerform,
        MayNotPerform
    }

    /**
     * Role configurations keyed by role name. The map key is the role name, so a
     * role entry no longer needs to repeat it, e.g.
     * {@code gsrs.security.info.roles.DataEntry.privileges = ["Edit"]}.
     */
    private Map<String, RoleConfiguration> roles = new LinkedHashMap<>();

}
