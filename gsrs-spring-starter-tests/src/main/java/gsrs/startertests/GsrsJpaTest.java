package gsrs.startertests;

import gsrs.AuditConfig;
import gsrs.GsrsFactoryConfiguration;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.jupiter.*;
import ix.core.models.Principal;
import ix.core.search.text.Lucene4IndexServiceFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.*;

/**
 * Annotation for a  GSRS JPA Test.  This sets up some common GSRS configurations
 * ontop of {@link DataJpaTest} such as clearing GSRS audit info, and text indexing etc.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ContextConfiguration
@DirtiesContext
@DataJpaTest
@Import({ClearAuditorBeforeEachExtension.class , ClearTextIndexerExtension.class,  AuditConfig.class, AutowireHelper.class,
//        PrincipalRepository.class,
//        ResetAllCacheSupplierBeforeEachExtension.class,
//        ResetAllEntityProcessorBeforeEachExtension.class, ResetAllEntityServicesBeforeEachExtension.class,
        GsrsFactoryConfiguration.class,
//        TextIndexerFactory.class, TextIndexerConfig.class,
//        Lucene4IndexServiceFactory.class,
        Principal.class,
        Lucene4IndexServiceFactory.class,
        GsrsEntityTestConfiguration.class})
public @interface GsrsJpaTest {
    /**
     * The dirties context tells the test
     * when not only to recreate the h2 database (and any other context related thing)
     * this not only wipes out the loaded data but resets all auto increment counters.
     * By default, this will wipe out the data after each test method is {@link org.springframework.test.annotation.DirtiesContext.ClassMode#AFTER_CLASS}
     * but consider changing it to
     *{@link org.springframework.test.annotation.DirtiesContext.ClassMode#AFTER_EACH_TEST_METHOD}
     * if you have tests that need to reset the auto increment ids for each test.
     *
     * @return
     */
    @AliasFor(annotation = DirtiesContext.class, attribute = "classMode")
    DirtiesContext.ClassMode dirtyMode() default DirtiesContext.ClassMode.AFTER_CLASS;

    @AliasFor(annotation = ContextConfiguration.class)
    Class[] classes() default {};

    @AliasFor(
            annotation = ImportAutoConfiguration.class,
            attribute = "exclude"
    )
    Class<?>[] excludeAutoConfiguration() default {};
}
