package gsrs.startertests.junit4;


import gsrs.AuditConfig;
import gsrs.springUtils.AutowireHelper;
import org.junit.Rule;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;

/**
 * Abstract class that autoregisters some GSRS Junit 4 Rules
 * such as {@link ClearTextIndexerRule} and {@link ClearAuditorRule}.  This also changes
 * the property for `ix.home` which is used by the LegacyTextIndexer to
 * make the TextIndexer write the index to a temporary folder for each test.
 */
@ContextConfiguration(initializers = AbstractGsrsJpaEntityJunit4Test.Initializer.class)
@Import({ClearAuditorRule.class , ClearTextIndexerRule.class,  AuditConfig.class, AutowireHelper.class,

})
public abstract class AbstractGsrsJpaEntityJunit4Test {


    @TempDir
    static File tempDir;


    @Autowired
    @Rule
    protected ClearTextIndexerRule clearTextIndexerRule;

    @Autowired
    @Rule
    public ClearAuditorRule clearAuditorRule;

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "ix.home=" + tempDir
            ).applyTo(context);
        }
    }
}
