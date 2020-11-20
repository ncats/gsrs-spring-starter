package gsrs.entityProcessor;

import gsrs.BasicEntityProcessorFactory;
import gsrs.EntityProcessorFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

@Configuration
public class BasicEntityProcessorConfiguration {
    @Bean
    @Primary
    public EntityProcessorFactory entityProcessorFactory(){
        return new BasicEntityProcessorFactory();
    }
}
