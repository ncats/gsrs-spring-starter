package gsrs.startertests.jupiter;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.indexer.IndexValueMakerFactory;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestComponent;

/**
 * Calls  {@link IndexValueMakerFactory#INDEX_VALUE_MAKER_INTIALIZATION_GROUP#resetCache();} before each JUnit 5 test is run.
 *
 * <pre>
 *  {@code
 *  @RegisterExtension
 *  ResetIndexValueMakerFactoryBeforeEachExtension resetIndexValueMakerFactory = new ResetIndexValueMakerFactoryBeforeEachExtension();
 *  }
 *  </pre>
 *
 *
 */
@TestComponent
public class ResetIndexValueMakerFactoryBeforeEachExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        IndexValueMakerFactory.INDEX_VALUE_MAKER_INTIALIZATION_GROUP.resetCache();
    }
}
