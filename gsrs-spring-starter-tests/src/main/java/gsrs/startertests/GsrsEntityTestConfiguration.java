package gsrs.startertests;

import gsrs.EntityProcessorFactory;
import gsrs.GsrsFactoryConfiguration;
import gsrs.indexer.IndexValueMakerFactory;
import ix.core.search.text.Lucene4IndexServiceFactory;
import ix.core.search.text.TextIndexerConfig;
import ix.core.search.text.TextIndexerFactory;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.context.ContextConfiguration;

@TestConfiguration
@ContextConfiguration(classes= {GsrsFactoryConfiguration.class,
        TextIndexerFactory.class, TextIndexerConfig.class,
        Lucene4IndexServiceFactory.class})
@Order
public class GsrsEntityTestConfiguration {



    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public EntityProcessorFactory defaultEntityProcessorFactory(){
        return new TestEntityProcessorFactory();
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnMissingBean
    public IndexValueMakerFactory defaultIndexValueMakerFactory(){
        return new TestIndexValueMakerFactory();
    }

}

