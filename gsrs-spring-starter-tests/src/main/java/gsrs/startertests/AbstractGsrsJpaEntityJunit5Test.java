package gsrs.startertests;


import gsrs.AuditConfig;
import gsrs.springUtils.AutowireHelper;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;

/**
 * Abstract class that autoregisters some GSRS Junit 5 Extensions (what Junit 4 called "Rules")
 * such as {@link ClearTextIndexerRule} and {@link ClearAuditorRule}.  This also changes
 * the property for `ix.home` which is used by the LegacyTextIndexer to
 * make the TextIndexer write the index to a temporary folder for each test.
 */
@ContextConfiguration(initializers = AbstractGsrsJpaEntityJunit5Test.Initializer.class)
@Import({ClearAuditorRule.class , ClearTextIndexerRule.class,  AuditConfig.class, AutowireHelper.class, ResetAllCacheSupplierExtension.class})
public abstract class AbstractGsrsJpaEntityJunit5Test {


    @TempDir
    static File tempDir;


    @Autowired
    @RegisterExtension
    protected ClearTextIndexerRule clearTextIndexerRule;

    @Autowired
    @RegisterExtension
    protected ClearAuditorRule clearAuditorRule;

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "ix.home=" + tempDir
            ).applyTo(context);
        }
    }
}
