package gov.nih.ncats.gsrsdiscovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GsrsDiscoveryApplicationTests {

    @Test   //This test works with Java 8 and Java 11, but not with Java 17. Jira ticket: NGSRS-413 has more details.
    void contextLoads() {
    }

}
