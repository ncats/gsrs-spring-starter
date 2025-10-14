package gsrs.startertests;

import gsrs.EntityProcessorFactory;
import gsrs.GsrsFactoryConfiguration;
import gsrs.autoconfigure.GsrsApiAutoConfiguration;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.entityProcessor.ConfigBasedEntityProcessorFactory;
import gsrs.indexer.DefaultIndexerEventFactoryFactory;
import gsrs.indexer.IndexValueMakerFactory;
import gsrs.indexer.IndexerEventFactoryFactory;
import gsrs.services.PrivilegeService;
import gsrs.validator.GsrsValidatorFactory;
import ix.core.search.bulk.UserSavedListService;
import ix.core.search.text.Lucene4IndexServiceFactory;
import ix.core.search.text.TextIndexerConfig;
import ix.core.search.text.TextIndexerFactory;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.context.ContextConfiguration;

@TestConfiguration
@ContextConfiguration(classes= {GsrsFactoryConfiguration.class, GsrsApiAutoConfiguration.class,
        TextIndexerFactory.class, TextIndexerConfig.class,
        Lucene4IndexServiceFactory.class, UserSavedListService.class, PrivilegeService.class})
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


    @Bean
    @Order
    @ConditionalOnMissingBean
    @Primary
    public GsrsValidatorFactory defaultValidatorFactory(){
        return new TestGsrsValidatorFactory();
    }
    @Bean
    @Order
    @ConditionalOnMissingBean
    public IndexerEventFactoryFactory indexerEventFactoryFactory(){
        return new DefaultIndexerEventFactoryFactory();
    }

    @Bean
    @Order
    @ConditionalOnMissingBean
    public UserSavedListService userSavedListService() {return new UserSavedListService();}
}

