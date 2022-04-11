package gsrs.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.GsrsFactoryConfiguration;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.validator.ValidatorConfig;
import ix.ginas.utils.validation.ValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import java.util.List;

public class ConfigBasedGsrsImportAdapterFactory implements GsrsImportAdapterFactory {
    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;

    @Bean
    @Primary
    @ConditionalOnMissingBean
    @Order
    public ConfigBasedGsrsImportAdapterFactory newFactory(String context) {
        List<? extends ImportAdapterFactoryConfig> configs = gsrsFactoryConfiguration.getImportAdapterFactories(context);
        return new ConfigBasedGsrsImportAdapterFactory();
    }
}
