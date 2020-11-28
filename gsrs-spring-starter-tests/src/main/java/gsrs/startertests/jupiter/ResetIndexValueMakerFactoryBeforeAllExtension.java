package gsrs.startertests.jupiter;

import gsrs.indexer.IndexValueMakerFactory;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestComponent;

/**
 * Calls  {@link IndexValueMakerFactory#INDEX_VALUE_MAKER_INTIALIZATION_GROUP#resetCache();} before all JUnit 5 tests are run.
 *
 * <pre>
 *  {@code
 *  @RegisterExtension
 *  ResetIndexValueMakerFactoryBeforeAllExtension resetIndexValueMakerFactory = new ResetIndexValueMakerFactoryBeforeAllExtension();
 *  }
 *  </pre>
 *
 *
 */
@TestComponent
public class ResetIndexValueMakerFactoryBeforeAllExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        IndexValueMakerFactory.INDEX_VALUE_MAKER_INTIALIZATION_GROUP.resetCache();
    }
}
