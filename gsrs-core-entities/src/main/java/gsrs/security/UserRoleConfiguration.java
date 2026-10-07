package gsrs.security;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
    private List<RoleConfiguration> roles;

}
