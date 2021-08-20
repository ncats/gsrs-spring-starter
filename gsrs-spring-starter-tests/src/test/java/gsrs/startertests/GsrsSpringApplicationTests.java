package gsrs.startertests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import gsrs.DefaultDataSourceConfig;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;

@ActiveProfiles("test")
@SpringBootTest(classes = {GsrsSpringApplication.class,  GsrsEntityTestConfiguration.class})
class GsrsSpringApplicationTests extends AbstractGsrsJpaEntityJunit5Test {


    @Test
    void contextLoads() {
    }

}
