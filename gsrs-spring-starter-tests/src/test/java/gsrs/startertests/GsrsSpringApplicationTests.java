package gsrs.startertests;

import gsrs.AuditConfig;
import gsrs.springUtils.AutowireHelper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = {GsrsSpringApplication.class,  GsrsEntityTestConfiguration.class})
class GsrsSpringApplicationTests extends AbstractGsrsJpaEntityJunit5Test{


    @Test
    void contextLoads() {
    }

}
