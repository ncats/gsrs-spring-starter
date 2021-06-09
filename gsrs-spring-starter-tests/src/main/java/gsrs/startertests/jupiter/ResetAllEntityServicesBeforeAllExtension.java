package gsrs.startertests.jupiter;

import gsrs.service.GsrsEntityService;
import gsrs.startertests.GsrsJpaTest;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestComponent;

/**
 * Calls  {@link GsrsEntityService#ENTITY_SERVICE_INTIALIZATION_GROUP#resetCache()}
 * before all JUnit 5 tests are run.
 * If called from a test class that extends {@link AbstractGsrsJpaEntityJunit5Test}
 * or from an annotated class with {@link GsrsJpaTest} annotation,
 * you can autowire it:
 * <pre>
 * {@code
 * @Autowired
 * @RegisterExtension
 * ResetAllEntityServicesBeforeAllExtension resetAllEntityServicesExtension;
 * }
 * </pre>
 *
 * otherwise you have to create it:
 *
 * <pre>
 *  {@code
 *  @RegisterExtension
 *  ResetAllEntityServicesBeforeAllExtension resetAllEntityServicesExtension = new ResetAllEntityServicesBeforeAllExtension();
 *  }
 *  </pre>
 *
 *
 */
@TestComponent
public class ResetAllEntityServicesBeforeAllExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        GsrsEntityService.ENTITY_SERVICE_INTIALIZATION_GROUP.resetCache();
    }
}
