package gsrs.startertests;

import gsrs.AuditConfig;
import gsrs.GsrsFactoryConfiguration;
import gsrs.repository.PrincipalRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.search.text.Lucene4IndexServiceFactory;
import ix.core.search.text.TextIndexerConfig;
import ix.core.search.text.TextIndexerFactory;
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
@ContextConfiguration(classes = {GsrsSpringApplication.class, PrincipalRepository.class,
        GsrsFactoryConfiguration.class, GsrsEntityTestConfiguration.class,

        Lucene4IndexServiceFactory.class})
@DirtiesContext
@DataJpaTest
//this dirties context makes us recreate the h2 database after each method (and any other context related thing)
//this not only wipes out the loaded data but resets all auto increment counters.
//without this even if we remove all entities from the repository after each test, the ids wouldn't reset back to 1
@Import({ClearAuditorRule.class , ClearTextIndexerRule.class,  AuditConfig.class, AutowireHelper.class})
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
}
