package gsrs.startertests.jupiter;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.startertests.GsrsJpaTest;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestComponent;

/**
 * Calls  {@link CachedSupplier#resetAllCaches()} before all the JUnit 5 tests are run.
 * If called from a test class that extends {@link AbstractGsrsJpaEntityJunit5Test}
 * or from an annotated class with {@link GsrsJpaTest} annotation,
 * you can autowire it:
 * <pre>
 * {@code
 * @Autowired
 * @RegisterExtension
 * ResetAllCacheSupplierBeforeAllExtension resetAllCacheSupplierExtension;
 * }
 * </pre>
 *
 * otherwise you have to create it:
 *
 * <pre>
 *  {@code
 *  @RegisterExtension
 *  ResetAllCacheSupplierBeforeAllExtension resetAllCacheSupplierExtension = new ResetAllCacheSupplierBeforeAllExtension();
 *  }
 *  </pre>
 *
 *
 */
@TestComponent
public class ResetAllCacheSupplierBeforeAllExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        CachedSupplier.resetAllCaches();
    }
}
