package gsrs.startertests;

import gsrs.controller.GsrsControllerConfiguration;
import ix.core.search.text.TextIndexerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@GsrsJpaTest( classes = { GsrsSpringApplication.class, GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class})
class GsrsSpringApplicationTests extends AbstractGsrsJpaEntityJunit5Test {

	@MockitoBean
	WebMvcRegistrations webMvcRegistrations;

	@Test
	void contextLoads() {
    }

}
