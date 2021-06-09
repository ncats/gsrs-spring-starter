package gsrs.startertests.junit4;

import gsrs.indexer.IndexValueMakerFactory;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import org.springframework.boot.test.context.TestComponent;

/**
 * Calls  {@link IndexValueMakerFactory#INDEX_VALUE_MAKER_INTIALIZATION_GROUP#resetCache();}
 * as a JUnit 4 Rule.
 *
 * <pre>
 *  {@code
 *  @Rule
 *  public ResetIndexValueMakerFactoryRule resetIndexValueMakerFactory = new ResetIndexValueMakerFactoryRule();
 *  }
 *  </pre>
 *
 *
 */
@TestComponent
public class ResetIndexValueMakerFactoryRule extends ExternalResource {

    @Override
    public void before() {
        IndexValueMakerFactory.INDEX_VALUE_MAKER_INTIALIZATION_GROUP.resetCache();
    }
}
