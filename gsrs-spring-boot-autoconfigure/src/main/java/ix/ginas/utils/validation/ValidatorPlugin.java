package ix.ginas.utils.validation;

import gsrs.validator.DefaultValidatorConfig;
import ix.core.validator.Validator;
import ix.core.validator.ValidatorCategory;

/**
 * Created by katzelda on 5/7/18.
 */
public interface ValidatorPlugin<T> extends Validator<T> {

    boolean supports(T newValue, T oldValue, DefaultValidatorConfig.METHOD_TYPE methodType);


    default boolean supportsCategory(T newValue, T oldValue, ValidatorCategory cat) {
        if(ValidatorCategory.CATEGORY_ALL().equals(cat)) {
            return true;
        }else {
            return false;
        }
    }
    


}
