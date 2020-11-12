package ix.ginas.utils.validation;

import gsrs.validator.ValidatorConfig;
import ix.core.validator.Validator;

/**
 * Created by katzelda on 5/7/18.
 */
public interface ValidatorPlugin<T> extends Validator<T> {

    boolean supports(T newValue, T oldValue, ValidatorConfig.METHOD_TYPE methodType);




}
