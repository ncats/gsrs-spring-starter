package gsrs.startertests;

import gsrs.BasicEntityProcessorFactory;
import gsrs.GsrsFactoryConfiguration;
import ix.core.search.text.Lucene4IndexServiceFactory;
import ix.core.search.text.TextIndexerConfig;
import ix.core.search.text.TextIndexerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import( {GsrsFactoryConfiguration.class,
        BasicEntityProcessorFactory.class, TextIndexerFactory.class, TextIndexerConfig.class,
        Lucene4IndexServiceFactory.class})
public class GsrsEntityTestConfiguration {


}
