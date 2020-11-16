package gsrs;

import gsrs.entityProcessor.EntityProcessorConfig;
import gsrs.validator.ValidatorConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
@Component
@ConfigurationProperties("gsrs")
@Data
public class GsrsFactoryConfiguration {

    private Map<String, List<ValidatorConfig>> validators;
    private Map<String, List<EntityProcessorConfig>> entityprocessors;


    public List<EntityProcessorConfig> getEntityProcessorConfigByContext(String context){
        if(validators ==null){
            //nothing set
            return Collections.emptyList();
        }
        return entityprocessors.getOrDefault(context, Collections.emptyList());
    }

    public List<ValidatorConfig> getValidatorConfigByContext(String context){
        if(validators ==null){
            //nothing set
            return Collections.emptyList();
        }
        return validators.getOrDefault(context, Collections.emptyList());
    }
}
