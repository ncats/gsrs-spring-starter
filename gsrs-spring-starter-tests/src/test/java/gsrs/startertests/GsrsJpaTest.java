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

@DataJpaTest
@Import({ClearAuditorRule.class , ClearTextIndexerRule.class,  AuditConfig.class, AutowireHelper.class})
public @interface GsrsJpaTest {
}
