package gsrs.entityProcessor;

import gsrs.EntityProcessorFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

@Configuration
public class ConfigBasedEntityProcessorConfiguration {
    @Bean
    @Primary
    @ConditionalOnMissingBean
    @Order
    public EntityProcessorFactory entityProcessorFactory(){
        return new ConfigBasedEntityProcessorFactory();
    }
}
