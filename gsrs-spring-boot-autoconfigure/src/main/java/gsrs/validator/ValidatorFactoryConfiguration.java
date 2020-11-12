package gsrs.validator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
@Component
@ConfigurationProperties("gsrs")
@Data
public class ValidatorFactoryConfiguration {

    private Map<String, List<ValidatorConfig>> validators;
//    private Map<String, Object> validators;

    private String example;

    public List<ValidatorConfig> getValidatorConfigByContext(String context){
        if(validators ==null){
            //nothing set
            return Collections.emptyList();
        }
        System.out.println("found validators!");
        return validators.getOrDefault(context, Collections.emptyList());
    }
}
