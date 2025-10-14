package gsrs.startertests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest(
	classes = {GsrsSpringApplication.class,  GsrsEntityTestConfiguration.class},
	properties = {"spring.application.name=starter"}
)

class GsrsSpringApplicationTests extends AbstractGsrsJpaEntityJunit5Test {

	@MockitoBean
	WebMvcRegistrations webMvcRegistrations;

	@Test
	void contextLoads() {
    }

}
