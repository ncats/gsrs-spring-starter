package gsrs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@Profile("!test")

public class BasicServiceInfoController {

    // This controller is meant to provide endpoints to services that don't have to import the gsrs-spring-starter, e.g. discovery, gateway, frontend.

    @Value("#{new Boolean('${gsrs.extensions.config.report.api.enabled:false}')}")
    private boolean extensionsConfigReportApiEnabled;

    @Autowired
    GsrsServiceInfoEndpointPathConfiguration gsrsServiceInfoEndpointPathConfiguration;

    private static final MediaType jmt = MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE);
    private static final String notEnabledMessage = "{ \"message\" : \"Resource Not Enabled.\"}";
    private static final String noDataMessage = "{\"message\": \"A null or empty value was returned, or " +
    "an error occurred. This can happen if configs have not yet been populated or if there are no config " +
    "objects corresponding to the serviceContext (and/or entityContext) provided. In some cases, API " +
    "actions trigger the population of the data into cached suppliers or autowired values. These values " +
    "are null or empty until populated. In other cases, data is populated when the service starts. See " +
    "the doc: 'How Configuration Works' for some more detail.\"}";

    @GetMapping("/service-info/api/v1/{serviceContext}/@extensionConfigsInfoPaths")
    public ResponseEntity<?> getPaths()  {
        List<? extends ServiceInfoEndpointPathConfig> list = null;
        Map<String, List<? extends ServiceInfoEndpointPathConfig>> endpoints = new HashMap<>();
        boolean thrown = false;
        try {
            list = gsrsServiceInfoEndpointPathConfiguration.getEndpointsList();
        } catch (Throwable t) {
            thrown = true;
        }
        if (thrown || list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).contentType(jmt).body(noDataMessage);
        }
        endpoints.put("endpoints", list);
        return ResponseEntity.status(HttpStatus.OK).body(endpoints);
    }
}
