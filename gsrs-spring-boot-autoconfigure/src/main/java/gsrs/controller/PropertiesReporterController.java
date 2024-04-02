package gsrs.controller;

import gsrs.config.PropertiesReporter;
import gsrs.security.hasAdminRole;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;

@Slf4j
@Data
@RestController
public class PropertiesReporterController {

    @Autowired
    private ConfigurableEnvironment configurableEnvironment;

    @Value("${gsrs.services.config.properties.report.api.enabled:false}")
    private boolean propertiesApiReportEnabled;

    @Value("${gsrs.services.config.properties.report.log.enabled:false}")
    private boolean propertiesLogReportEnabled;

    @Value("${gsrs.services.knownServices")
    private ArrayList<String> knownServices;

    private final String NOT_A_KNOWN_SERVICE = "The context specified in the URL is not a known service.";
    private final String REPORT_NOT_ENABLED  = "This report is not enabled in the services configuration.";
    private final String PlEASE_CHECK_LOG_FOR_PROPERTIES  = "Please check log to view the service's configuration properties as %s";


//    @RequestMapping(value = "/process/{json}", method = RequestMethod.GET)
//    public ResponseEntity<?> process(@PathVariable("json") boolean processJson) {
//        if (processJson) {
//            return new ResponseEntity<>("someJSONObject", headers, HttpStatus.OK);
//        } else {
//            final HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_XML);
//            return new ResponseEntity<>("someXMLObject", headers, HttpStatus.OK);
//        }
//    }

//    @hasAdminRole
//    @GetMapping(value = "service-info/api/v1/{context}/@configurationProperties")
//    public String apiGetConfigurationPropertiesReport(
//        @PathVariable(value="context") String context,
//            @RequestParam(defaultValue = "text", required = false) String format
//    ) {
//        final HttpHeaders headers = new HttpHeaders();
//        if (format.equals("json")) {
//            headers.setContentType(MediaType.valueOf(MediaType.TEXT_PLAIN_VALUE));
//            if (propertiesApiReportEnabled) {
//                return PropertiesReporter.getConfigurationPropertiesAsText(configurableEnvironment);
//            }
//            return "This report is not enabled in the service's configuration.";
//        } else {
//            headers.setContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE));
//            if (propertiesApiReportEnabled) {
//                return PropertiesReporter.getConfigurationPropertiesAsJson(configurableEnvironment);
//            }
//            return "{\"message\": \"This report is not enabled in the services configuration.\"}";
//        }
//    }


    @hasAdminRole
    @GetMapping(value = "service-info/api/v1/{context}/@configurationPropertiesAsText", produces = MediaType.TEXT_PLAIN_VALUE)
    public @ResponseBody
        String apiGetConfigurationPropertiesReportAsText(@PathVariable(value="context") String context) {
        if (!inKnownServicesCheck(context)) {
            return NOT_A_KNOWN_SERVICE;
        }
        if (propertiesApiReportEnabled) {
            return PropertiesReporter.getConfigurationPropertiesAsText(configurableEnvironment);
        }
        return REPORT_NOT_ENABLED;
    }

    @hasAdminRole
    @GetMapping(value="/service-info/api/v1/{context}/@configurationPropertiesAsJson", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    String apiGetConfigurationPropertiesReportAsJson(@PathVariable(value="context") String context) {
        if (!inKnownServicesCheck(context)) {
            return "{\"message\":" + NOT_A_KNOWN_SERVICE + "}";
        }
        if (propertiesApiReportEnabled) {
            return PropertiesReporter.getConfigurationPropertiesAsJson(configurableEnvironment);
        }
        return "{\"message\":" + REPORT_NOT_ENABLED + "}";
    }

    @hasAdminRole
    @GetMapping(value="service-info/api/v1/{context}/@logConfigurationPropertiesAsText", produces = MediaType.TEXT_PLAIN_VALUE)
    public @ResponseBody
    String logConfigurationPropertiesAsText(@PathVariable(value="context") String context) {
        if (!inKnownServicesCheck(context)) {
            return NOT_A_KNOWN_SERVICE;
        }
        if (propertiesLogReportEnabled) {
            log.info(PropertiesReporter.getConfigurationPropertiesAsText(configurableEnvironment));
            return "{\"message\":" +String.format(PlEASE_CHECK_LOG_FOR_PROPERTIES, "text") +"}";
        }
        return REPORT_NOT_ENABLED;
    }

    @hasAdminRole
    @GetMapping(value="service-info/api/v1/{context}/@logConfigurationPropertiesAsJson", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    String logConfigurationPropertiesAsJson(@PathVariable(value="context") String context) {
        if (!inKnownServicesCheck(context)) {
            return NOT_A_KNOWN_SERVICE;
        }
        if (propertiesLogReportEnabled) {
            String message = String.format(PlEASE_CHECK_LOG_FOR_PROPERTIES, "JSON");
            log.info( message + "\n" + PropertiesReporter.getConfigurationPropertiesAsJson(configurableEnvironment));
            return "{\"message\":" + message + "}";
        }
        return "{\"message\":" + REPORT_NOT_ENABLED + "}";
    }

    public boolean inKnownServicesCheck(String service) {
        if (knownServices==null || knownServices.isEmpty()) return false;
        return knownServices.contains(service);
    }
}
