package gsrs.startertests.jupiter;

import gsrs.service.GsrsEntityService;
import gsrs.startertests.GsrsJpaTest;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestComponent;

/**
 * Calls  {@link GsrsEntityService#ENTITY_SERVICE_INTIALIZATION_GROUP#resetCache()} before each JUnit 5 test is run.
 * If called from a test class that extends {@link AbstractGsrsJpaEntityJunit5Test}
 * or from an annotated class with {@link GsrsJpaTest} annotation,
 * you can autowire it:
 * <pre>
 * {@code
 * @Autowired
 * @RegisterExtension
 * ResetAllEntityServicesBeforeEachExtension resetAllEntityServicesExtension;
 * }
 * </pre>
 *
 * otherwise you have to create it:
 *
 * <pre>
 *  {@code
 *  @RegisterExtension
 *  ResetAllEntityServicesBeforeEachExtension resetAllEntityServicesExtension = new ResetAllEntityServicesBeforeEachExtension();
 *  }
 *  </pre>
 *
 *
 */
@TestComponent
public class ResetAllEntityServicesBeforeEachExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        GsrsEntityService.ENTITY_SERVICE_INTIALIZATION_GROUP.resetCache();
    }
}
