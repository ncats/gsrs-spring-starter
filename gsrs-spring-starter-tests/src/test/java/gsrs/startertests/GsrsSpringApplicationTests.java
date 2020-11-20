package gsrs.startertests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(classes = {GsrsSpringApplication.class,  GsrsEntityTestConfiguration.class})

class GsrsSpringApplicationTests {


    @Test
    void contextLoads() {
    }

}
