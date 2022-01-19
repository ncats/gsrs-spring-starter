package gsrs.startertests.validator;

import gsrs.validator.DefaultValidatorConfig;
import ix.core.validator.ValidatorCallback;
import ix.ginas.utils.validation.ValidatorPlugin;
import lombok.Data;

@Data
public class MyValidator implements ValidatorPlugin<Object> {

    private String foo;

    @Override
    public void validate(Object objnew, Object objold, ValidatorCallback callback) {

    }

    @Override
    public boolean supports(Object newValue, Object oldValue, DefaultValidatorConfig.METHOD_TYPE methodType) {
        return false;
    }
}
