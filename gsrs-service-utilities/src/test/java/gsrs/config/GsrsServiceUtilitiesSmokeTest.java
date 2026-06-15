package gsrs.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GsrsServiceUtilitiesSmokeTest {

    @Test
    void configurationPropertiesReturnsForbiddenWhenApiIsDisabled() {
        ServiceInfoController controller = new ServiceInfoController();
        controller.setPropertiesReportApiEnabled(false);

        ResponseEntity<?> response = controller.configurationProperties("ctx", "json");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        assertEquals("Resource not enabled.", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void logConfigurationPropertiesReturnsForbiddenWhenLogEndpointIsDisabled() {
        ServiceInfoController controller = new ServiceInfoController();
        controller.setPropertiesReportLogEnabled(false);

        ResponseEntity<?> response = controller.logConfigurationProperties("ctx", "json");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        assertEquals("Resource not enabled.", ((Map<?, ?>) response.getBody()).get("message"));
    }
}
