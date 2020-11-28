package gsrs.startertests.jupiter;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.startertests.GsrsJpaTest;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestComponent;

/**
 * Calls  {@link CachedSupplier#resetAllCaches()} before each JUnit 5 test is run.
 *
 * <pre>
 *  {@code
 *  @RegisterExtension
 *  ResetAllCacheSupplierExtension resetAllCacheSupplierExtension = new ResetAllCacheSupplierExtension();
 *  }
 *  </pre>
 *
 *
 */
@TestComponent
public class ResetAllCacheSupplierBeforeEachExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        CachedSupplier.resetAllCaches();
    }
}
