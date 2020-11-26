package gsrs.validator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
/**
 * a {@link GsrsValidatorFactory} that uses the conf
 * to find  the validators to use.
 */
@Configuration
public class ConfigBasedValidatorFactoryConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    @Order
    public GsrsValidatorFactory validatorFactory(){
        return new ConfigBasedGsrsValidatorFactory();
    }
}
