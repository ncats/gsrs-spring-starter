package gsrs.startertests;

import gov.nih.ncats.common.util.CachedSupplier;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestComponent;

/**
 * Calls  {@link CachedSupplier#resetAllCaches()} before each JUnit 5 test is run.
 * If called from a test class that extends {@link AbstractGsrsJpaEntityJunit5Test}
 * or from an annotated class with {@link GsrsJpaTest} annotation,
 * you can autowire it:
 * <pre>
 * {@code
 * @Autowired
 * @RegisterExtension
 * ResetAllCacheSupplierExtension resetAllCacheSupplierExtension;
 * }
 * </pre>
 *
 * otherwise you have to create it:
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
public class ResetAllCacheSupplierExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        CachedSupplier.resetAllCaches();
    }
}
