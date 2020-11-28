package gsrs.startertests.jupiter;

import gsrs.EntityProcessorFactory;
import gsrs.startertests.GsrsJpaTest;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestComponent;

/**
 * Calls  {@link EntityProcessorFactory#ENTITY_PROCESSOR_FACTORY_INITIALIZER_GROUP#resetCache()}
 * before all JUnit 5 tests are run.
 *
 * <pre>
 *  {@code
 *  @RegisterExtension
 *  ResetAllEntityProcessorBeforeAllExtension resetAllEntityServicesExtension = new ResetAllEntityProcessorBeforeAllExtension();
 *  }
 *  </pre>
 *
 *
 */
public class ResetAllEntityProcessorBeforeAllExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext){
        EntityProcessorFactory.ENTITY_PROCESSOR_FACTORY_INITIALIZER_GROUP.resetCache();
    }
}
