package gsrs.entityProcessor;

import gsrs.EntityProcessorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ConfigBasedEntityProcessorConfiguration {
    @Bean
    @Primary
    public EntityProcessorFactory entityProcessorFactory(){
        return new ConfigBasedEntityProcessorFactory();
    }
}
