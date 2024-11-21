package gsrs.config;

// import gsrs.security.GsrsSecurityUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import java.util.*;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverterBuilder;

// import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

// https://mvnrepository.com/artifact/com.fasterxml.jackson.dataformat/jackson-dataformat-properties
// https://www.adam-bien.com/roller/abien/entry/java_ee_8_converting_java
// https://github.com/mikolajmitura/java-properties-to-json

// https://stackoverflow.com/questions/23506471/access-all-environment-properties-as-a-map-or-properties-object
// https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/env/ConfigurableEnvironment.html

@Slf4j
@Data
public class ConfigurationPropertiesChecker2 {

    public boolean enabled;

    public String getActivePropertiesAsJson(ConfigurableEnvironment configurableEnvironment) {

        // Aside from password, this is temporary to prevent exceptions.
        List<String> omissionFragments = Arrays.asList("password", "pattern", "regex", "relaxed");

        //  CONSIDER how to handle security
        // having problems calling this from gateway because it does not have gsrs-core-entities and including it
        // makes it want to have a database.
        boolean isAdmin = true;
        // boolean isAdmin = GsrsSecurityUtils.isAdmin();

        if (enabled && isAdmin) {
            List<MapPropertySource> propertySources = new ArrayList<MapPropertySource>();
            configurableEnvironment.getPropertySources().forEach(it -> {
                //&& it.getName().contains("applicationConfig")
                if (it instanceof MapPropertySource) {
                    propertySources.add((MapPropertySource) it);
                }
            });
            Map<String, Object> properties = new HashMap<>();

            propertySources.stream()
            .map(propertySource -> propertySource.getSource().keySet())
            .flatMap(Collection::stream)
            .distinct()
            .sorted()
            .forEach(key -> {
                // this if then is temporary
                if(!key.equals("java.version.date") && !key.contains("java.vendor")) {
                    try {
                        if (omissionFragments.stream().map(s -> s.toLowerCase()).anyMatch(key.toLowerCase()::contains)) {
                            properties.put(key, "[OMITTED VALUE]");
                        } else {
                            properties.put(key, configurableEnvironment.getProperty(key));
                        }
                    } catch (Exception e) {
                        log.warn("{} -> {}", key, e.getMessage());
                    }
                }
            });
            PropertiesToJsonConverter converter = PropertiesToJsonConverterBuilder.builder().build();
            String json = converter.convertFromValuesAsObjectMap(properties, false);
            return json;
        } else {
            return "{ \"message\": \"Unauthorized\"}";
        }
    }

    public String getActivePropertiesAsTextBlock(ConfigurableEnvironment configurableEnvironment) {

        List<String> omissionFragments = Arrays.asList("password");

        //  CONSIDER how to handle security
        boolean isAdmin = true;
        // isAdmin = GsrsSecurityUtils.isAdmin();

        if (enabled && isAdmin) {
            StringBuilder sb = new StringBuilder();
            List<MapPropertySource> propertySources = new ArrayList<MapPropertySource>();

            sb.append("**** BEGIN ACTIVE APP PROPERTIES ****").append("\n\n");
            sb.append("enabled: ").append(enabled).append("\n");
            sb.append("Groups:").append("\n\n");

            configurableEnvironment.getPropertySources().forEach(it -> {
                // it.getName().equals("systemEnvironment") &&
                if (it instanceof MapPropertySource) {
                    sb.append(it.getName()).append("\n");
                    propertySources.add((MapPropertySource) it);
                }
            });
            sb.append("\n\nProperties:").append("\n\n");
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

