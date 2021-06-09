package gsrs.startertests;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.validator.GsrsValidatorFactory;
import gsrs.validator.ValidatorConfig;
import ix.ginas.utils.validation.ValidatorFactory;

import java.util.*;

public class TestGsrsValidatorFactory implements GsrsValidatorFactory {
    private Map<String, List<ValidatorConfig>> validators;
    private ObjectMapper objectMapper = new ObjectMapper();

    public TestGsrsValidatorFactory(){
        validators = new HashMap<>();
    }

    public TestGsrsValidatorFactory setValidatorsForContext(String context, ValidatorConfig...validatorConfigs){
        //wrap the list in a new arrayList so we can add more later if asked
        validators.put(context, new ArrayList<>(Arrays.asList(validatorConfigs)));
        return this;
    }

    public List<ValidatorConfig> getValidatorsForContext(String context){
        return validators.getOrDefault(context, Collections.emptyList());
    }

    @Override
    public ValidatorFactory newFactory(String context) {
        return new ValidatorFactory(getValidatorsForContext(context), objectMapper);
    }

    public TestGsrsValidatorFactory addValidator(String context, ValidatorConfig config){
        validators.computeIfAbsent(context, k-> new ArrayList<>()).add(config);
        return this;
    }
}
