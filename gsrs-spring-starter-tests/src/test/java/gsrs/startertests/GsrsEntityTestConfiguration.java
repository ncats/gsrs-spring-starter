package gsrs.startertests;

import gsrs.EntityProcessorFactory;
import gsrs.GsrsFactoryConfiguration;
import gsrs.indexer.IndexValueMakerFactory;
import ix.core.search.text.Lucene4IndexServiceFactory;
import ix.core.search.text.TextIndexerConfig;
import ix.core.search.text.TextIndexerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@TestConfiguration
@Import( {GsrsFactoryConfiguration.class,
         TextIndexerFactory.class, TextIndexerConfig.class,
        Lucene4IndexServiceFactory.class})

public class GsrsEntityTestConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public EntityProcessorFactory entityProcessorFactory(){
        return new TestEntityProcessorFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public IndexValueMakerFactory indexValueMakerFactory(){
        return new TestIndexValueMakerFactory();
    }

}
