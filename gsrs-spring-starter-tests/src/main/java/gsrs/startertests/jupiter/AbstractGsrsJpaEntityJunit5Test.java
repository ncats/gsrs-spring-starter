package gsrs.startertests.jupiter;


import gsrs.AuditConfig;
import gsrs.EntityPersistAdapter;
import gsrs.springUtils.AutowireHelper;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;

/**
 * Abstract class that autoregisters some GSRS Junit 5 Extensions (what Junit 4 called "Rules")
 * such as {@link ClearTextIndexerExtension} and {@link ClearAuditorBeforeEachExtension}.  This also changes
 * the property for `ix.home` which is used by the LegacyTextIndexer to
 * make the TextIndexer write the index to a temporary folder for each test.
 */
@ContextConfiguration(initializers = AbstractGsrsJpaEntityJunit5Test.Initializer.class)
@Import({ClearAuditorBeforeEachExtension.class , ClearTextIndexerExtension.class,  AuditConfig.class, AutowireHelper.class,
        EntityPersistAdapter.class, EntityPersistAdapter.class
//        ResetAllCacheSupplierBeforeEachExtension.class, ResetAllCacheSupplierBeforeAllExtension.class,
//        ResetAllEntityProcessorBeforeEachExtension.class, ResetAllEntityProcessorBeforeAllExtension.class,
//        ResetAllEntityServicesBeforeEachExtension.class, ResetAllEntityServicesBeforeEachExtension.class
})
public abstract class AbstractGsrsJpaEntityJunit5Test {


    @TempDir
    protected static File tempDir;

//    @MockBean
    @Autowired
    protected EntityPersistAdapter epa;

    @Autowired
    @RegisterExtension
    protected ClearTextIndexerExtension clearTextIndexerRule;

    @Autowired
    @RegisterExtension
    protected ClearAuditorBeforeEachExtension clearAuditorRule;

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "ix.home=" + tempDir
            ).applyTo(context);
        }
    }
}
