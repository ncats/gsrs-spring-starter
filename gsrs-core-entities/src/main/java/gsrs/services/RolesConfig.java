package gsrs.services;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("roleConfig")
@ConfigurationProperties(prefix = "gsrs.roles")
@Data
public class RolesConfig {
    private String jsonFile;
}