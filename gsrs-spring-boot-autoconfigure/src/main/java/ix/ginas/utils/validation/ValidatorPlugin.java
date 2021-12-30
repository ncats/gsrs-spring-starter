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

    /**
     * perform any initialization or validation checks to make sure
     * the Validator is created correctly and all required fields are set.
     *  This method is called only once after the validator is created and autowired.
     * @throws IllegalStateException if there are any problems.
     */
    default void initialize() throws IllegalStateException{}
    


}
