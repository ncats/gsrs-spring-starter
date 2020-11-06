package gsrs.validator;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
@Component
@ConfigurationProperties("gsrs")
@Data
public class ValidatorFactoryConfiguration {

    private Map<String, List<ValidatorFactoryService.ValidatorConfig>> validators;
//    private Map<String, Object> validators;

    private String example;

    public List<ValidatorFactoryService.ValidatorConfig> getValidatorConfigByContext(String context){
        return validators.getOrDefault(context, Collections.emptyList());
    }
}
