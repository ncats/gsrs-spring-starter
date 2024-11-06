package gsrs.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import java.util.*;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverterBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;


@Slf4j
@Data
public class ConfigurationPropertiesChecker {

    public boolean enabled;

    public String getActivePropertiesAsJson(ConfigurableEnvironment configurableEnvironment) {

        // Aside from password, this is temporary to prevent exceptions.
        List<String> omissionFragments = Arrays.asList("password", "pattern", "regex", "relaxed");

        if (enabled) {
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
            String json = null;
            try {
                PropertiesToJsonConverter converter = PropertiesToJsonConverterBuilder.builder().build();
                json = converter.convertFromValuesAsObjectMap(properties, false);
            } catch (Exception e) {
                return "{ \"message\": \"Exception while converting properties to JSON.\"}";
            }
            try {
                return prettifyJson(json);
            } catch (JsonProcessingException jpe) {
                return "{ \"message\": \"Exception while prettifying JSON.\"}";
            }
        } else {
            return "{ \"message\": \"Resource is not enabled.\"}";
        }
    }

    private String prettifyJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Object jsonObject = mapper.readValue(json, Object.class);
        String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        return json;
    }

    public String getActivePropertiesAsTextBlock(ConfigurableEnvironment configurableEnvironment) {

        List<String> omissionFragments = Arrays.asList("password");

        if (enabled) {
            StringBuilder sb = new StringBuilder();
            List<MapPropertySource> propertySources = new ArrayList<MapPropertySource>();

            sb.append("**** BEGIN ACTIVE APP PROPERTIES ****").append("\n\n");
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
            return "Resource is not enabled.";
        }
    }
}

