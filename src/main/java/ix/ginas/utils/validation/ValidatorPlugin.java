package ix.ginas.utils.validation;

import gsrs.validator.ValidatorFactoryService;
import ix.core.validator.Validator;

/**
 * Created by katzelda on 5/7/18.
 */
public interface ValidatorPlugin<T> extends Validator<T> {

    boolean supports(T newValue, T oldValue, ValidatorFactoryService.ValidatorConfig.METHOD_TYPE methodType);




}
