package gov.nih.ncats.gsrsdiscovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GsrsDiscoverySmokeTest {

    @Test
    void discoveryApplicationHasExpectedBootAnnotations() {
        assertNotNull(GsrsDiscoveryApplication.class.getAnnotation(SpringBootApplication.class));
        assertNotNull(GsrsDiscoveryApplication.class.getAnnotation(EnableEurekaServer.class));
    }
}
