package gsrs.startertests.jupiter;

import gsrs.EntityProcessorFactory;
import gsrs.startertests.GsrsJpaTest;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestComponent;

/**
 * Calls  {@link EntityProcessorFactory#ENTITY_PROCESSOR_FACTORY_INITIALIZER_GROUP#resetCache()} before each JUnit 5 test is run.
 * If called from a test class that extends {@link AbstractGsrsJpaEntityJunit5Test}
 * or from an annotated class with {@link GsrsJpaTest} annotation,
 * you can autowire it:
 * <pre>
 * {@code
 * @Autowired
 * @RegisterExtension
 * ResetAllEntityProcessorBeforeEachExtension resetAllEntityServicesExtension;
 * }
 * </pre>
 *
 * otherwise you have to create it:
 *
 * <pre>
 *  {@code
 *  @RegisterExtension
 *  ResetAllEntityProcessorBeforeEachExtension resetAllEntityServicesExtension = new ResetAllEntityProcessorBeforeEachExtension();
 *  }
 *  </pre>
 *
 *
 */
@TestComponent
public class ResetAllEntityProcessorBeforeEachExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        EntityProcessorFactory.ENTITY_PROCESSOR_FACTORY_INITIALIZER_GROUP.resetCache();
    }
}
