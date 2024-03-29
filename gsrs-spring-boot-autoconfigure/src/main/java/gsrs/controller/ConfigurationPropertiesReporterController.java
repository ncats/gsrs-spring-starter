package gsrs.controller;

import gsrs.config.ConfigurationPropertiesReporter;
import gsrs.security.hasAdminRole;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Data
@RestController
public class ConfigurationPropertiesReporterController {

    @Autowired
    ConfigurableEnvironment configurableEnvironment;

    @Value("${gsrs.services.config.properties.report.api.enabled:false}")
    private boolean propertiesApiReportEnabled;

    @Value("${gsrs.services.config.properties.report.log.enabled:false}")
    private boolean propertiesLogReportEnabled;

    @hasAdminRole
    @GetMapping(value="/service-info/api/v1/{context}}/@configurationPropertiesAsText", produces = MediaType.TEXT_PLAIN_VALUE)
    public @ResponseBody
    String apiGetConfigurationPropertiesReportAsText(@PathVariable(value="context") String context) {
        if (propertiesApiReportEnabled) {
            return ConfigurationPropertiesReporter.getConfigurationPropertiesAsText(configurableEnvironment);
        }
        return "This report is not enabled in the services configuration.";
    }

    @hasAdminRole
    @GetMapping(value="/service-info/api/v1/{context}}/@configurationPropertiesAsJson", produces = MediaType.TEXT_PLAIN_VALUE)
    public @ResponseBody
    String apiGetConfigurationPropertiesReportAsJson(@PathVariable(value="context") String context) {
        if (propertiesApiReportEnabled) {
            return ConfigurationPropertiesReporter.getConfigurationPropertiesAsJson(configurableEnvironment);
        }
        return "{\"message\": \"This report is not enabled in the services configuration.\"}";
    }

    @hasAdminRole
    @GetMapping(value="service-info/api/v1/{context}}/@logConfigurationPropertiesAsText", produces = MediaType.TEXT_PLAIN_VALUE)
    public @ResponseBody
    String logConfigurationPropertiesAsText(@PathVariable(value="context") String context) {
        if (propertiesLogReportEnabled) {
            log.info(ConfigurationPropertiesReporter.getConfigurationPropertiesAsText(configurableEnvironment));
        }
        return "This report is not enabled in the services configuration.";
    }

    @hasAdminRole
    @GetMapping(value="service-info/api/v1/{context}}/@configurationPropertiesAsJson", produces = MediaType.TEXT_PLAIN_VALUE)
    public @ResponseBody
    String propertiesLogReportEnabled(@PathVariable(value="context") String context) {
        if (propertiesApiReportEnabled) {
            log.info("Logging configuration properties as Json:\n" + ConfigurationPropertiesReporter.getConfigurationPropertiesAsJson(configurableEnvironment));
        }
        return "\"Logging configuration properties as Json:\n" + "{\"message\": \"This report is not enabled in the services configuration.\"}\n";
    }
}
