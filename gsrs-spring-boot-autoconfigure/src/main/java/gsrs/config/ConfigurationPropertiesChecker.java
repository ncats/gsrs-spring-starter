package gsrs.config;

import gsrs.security.GsrsSecurityUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Slf4j
@Data

public class ConfigurationPropertiesChecker {

    public boolean enabled;

    // @Value("#{'${gsrs.config.configurationPropertyChecker.omissionFragments}'.split(',')}: ''")
    // public List<String> omissionFragments = Arrays.asList("password");

    public String getActivePropertiesAsTextBlock(ConfigurableEnvironment configurableEnvironment) {

        List<String> omissionFragments = Arrays.asList("password","abc");

        boolean isAdmin = true;
        // isAdmin = GsrsSecurityUtils.isAdmin();

        if (enabled && isAdmin) {
            StringBuilder sb = new StringBuilder();
            List<MapPropertySource> propertySources = new ArrayList<MapPropertySource>();

            sb.append("**** BEGIN ACTIVE APP PROPERTIES ****").append("\n\n");
            sb.append("eabled: ").append(enabled).append("\n");
            sb.append("Groups:").append("\n\n");

            configurableEnvironment.getPropertySources().forEach(it -> {
                //&& it.getName().contains("applicationConfig")
                if (it instanceof MapPropertySource) {
                    sb.append(it.getName()).append("\n");
                    propertySources.add((MapPropertySource) it);
                }
            });
            sb.append("Properties:").append("\n\n");
            propertySources.stream()
            .map(propertySource -> propertySource.getSource().keySet())
            .flatMap(Collection::stream)
            .distinct()
            .sorted()
            .forEach(key -> {
                try {
                    if(omissionFragments.stream().map(s->s.toLowerCase()).anyMatch(key.toLowerCase()::contains)) {
                         sb.append(key + "=").append("[OMITTED VALUE]\n");
                     } else {
                         sb.append(key + "=" + configurableEnvironment.getProperty(key)).append("\n");
                     }
                } catch (Exception e) {
                    log.warn("{} -> {}", key, e.getMessage());
                }
            });
            sb.append("**** END ACTIVE APP PROPERTIES ****");
            return sb.toString();
        } else {
            return "Unauthorized.";
        }
    }
}

