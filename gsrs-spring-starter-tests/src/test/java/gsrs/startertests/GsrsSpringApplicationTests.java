package gsrs.startertests;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.stagingarea.service.DefaultStagingAreaService;
import gsrs.stagingarea.service.StagingAreaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.test.context.ActiveProfiles;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@GsrsJpaTest( classes = { GsrsSpringApplication.class, GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class})
class GsrsSpringApplicationTests extends AbstractGsrsJpaEntityJunit5Test {

	@MockitoBean
	WebMvcRegistrations webMvcRegistrations;

	@Autowired
	StagingAreaService stagingAreaService;

	@Test
	void contextLoads() {
		Assertions.assertInstanceOf(DefaultStagingAreaService.class, stagingAreaService);
    }

}
