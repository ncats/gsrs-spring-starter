package gsrs.config;

import gsrs.security.hasAdminRole;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Data
@RestController
@RestControllerEndpoint(id = "stuff")
public class ExampleEndpointController2 {
    @Value("#{new Boolean('${gsrs.services.config.properties.report.api.enabled:false}')}")
    private boolean apiEnabled;

    @Autowired
    private Environment env;

    @Autowired
    private ConfigurableEnvironment configurableEnvironment;

    @hasAdminRole
    @GetMapping(value="/service-info/api/v1/{context}/@configurationPropertiesAsText", produces = MediaType.TEXT_PLAIN_VALUE)
    public @ResponseBody
        String checkConfigurationPropertiesAsText(@PathVariable(value="context") String context) {
        ConfigurationPropertiesChecker c = new ConfigurationPropertiesChecker();
        c.setEnabled(apiEnabled);
        return c.getActivePropertiesAsTextBlock(configurableEnvironment);
    }

    @hasAdminRole
    @GetMapping(value="/service-info/api/v1/{context}/@configurationPropertiesAsJson", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    String checkConfigurationPropertiesAsJson(@PathVariable(value="context") String context) {
        ConfigurationPropertiesChecker c = new ConfigurationPropertiesChecker();
        c.setEnabled(apiEnabled);
        return c.getActivePropertiesAsJson(configurableEnvironment);
    }
}
