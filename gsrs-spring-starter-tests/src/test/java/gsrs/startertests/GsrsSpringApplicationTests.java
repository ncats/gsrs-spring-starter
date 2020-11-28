package gsrs.startertests;

import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {GsrsSpringApplication.class,  GsrsEntityTestConfiguration.class})
class GsrsSpringApplicationTests extends AbstractGsrsJpaEntityJunit5Test {


    @Test
    void contextLoads() {
    }

}
