package gsrs.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.JsonTypeIdResolverConfiguration;
import gsrs.RegisteredFunctionProperties;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.indexer.IndexValueMakerFactory;
import gsrs.springUtils.AutowireHelper;
import gsrs.validator.ValidatorFactoryConfiguration;
import gsrs.validator.ValidatorFactoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
//can't do component scan in autoconfiguration so manually import our components
@Import(value = {AutowireHelper.class, GsrsControllerConfiguration.class,
        ValidatorFactoryService.class, ValidatorFactoryConfiguration.class,
        ValidatorFactoryService.class,
        JsonTypeIdResolverConfiguration.class, RegisteredFunctionProperties.class})
public class GsrsApiAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }
}
