package gsrs.imports;

import gsrs.validator.ConfigBasedGsrsValidatorFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

@Configuration
public class ConfigBasedImportAdapterFactoryFactoryConfiguration {
    @Bean
    @Primary
    @ConditionalOnMissingBean
    @Order
    public GsrsImportAdapterFactoryFactory importAdapterFactoryFactory(){
        return new ConfigBasedGsrsImportAdapterFactoryFactory();
    }

}
