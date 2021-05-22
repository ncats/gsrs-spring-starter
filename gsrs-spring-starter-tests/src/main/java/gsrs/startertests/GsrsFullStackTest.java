package gsrs.startertests;

import gsrs.AuditConfig;
import gsrs.EntityPersistAdapter;
import gsrs.GsrsFactoryConfiguration;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.jupiter.ClearAuditorBeforeEachExtension;
import gsrs.startertests.jupiter.ClearIxHomeExtension;
import ix.core.models.Principal;
import ix.core.search.text.Lucene4IndexServiceFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.*;

/**
 * Annotation for a  GSRS Test that loads the entire Spring boot stack including context configuration etc
 * BUT uses an h2 in memory test and like {@link GsrsJpaTest} it clearing GSRS audit info, and text indexing etc.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ContextConfiguration
@DirtiesContext
@AutoConfigureTestDatabase
@Import({ClearAuditorBeforeEachExtension.class , ClearIxHomeExtension.class,  AuditConfig.class, AutowireHelper.class,
//        PrincipalRepository.class,
//        ResetAllCacheSupplierBeforeEachExtension.class,
//        ResetAllEntityProcessorBeforeEachExtension.class, ResetAllEntityServicesBeforeEachExtension.class,
        GsrsFactoryConfiguration.class,
//        TextIndexerFactory.class, TextIndexerConfig.class,
//        Lucene4IndexServiceFactory.class,
        Principal.class,
        Lucene4IndexServiceFactory.class,
        GsrsEntityTestConfiguration.class,
        EntityPersistAdapter.class})
public @interface GsrsFullStackTest {
    /**
     * The dirties context tells the test
     * when not only to recreate the h2 database (and any other context related thing)
     * this not only wipes out the loaded data but resets all auto increment counters.
     * By default, this will wipe out the data after each test method is {@link DirtiesContext.ClassMode#AFTER_CLASS}
     * but consider changing it to
     *{@link DirtiesContext.ClassMode#AFTER_EACH_TEST_METHOD}
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
