package ix.ginas.utils.validation;

import gsrs.validator.DefaultValidatorConfig;

/**
 * An abstract implementation of {@link ValidatorPlugin}
 * to implement the {@link #supports(Object, Object, DefaultValidatorConfig.METHOD_TYPE)}.
 * @param <T>
 */
public abstract class AbstractValidatorPlugin<T> implements ValidatorPlugin<T>{
    /**
     * Supports all methodTypes as long as it's not set to IGNORE.
     * @param newValue
     * @param oldValue
     * @param methodType
     * @return {@code true} as long as the methodType is not set to {@link DefaultValidatorConfig.METHOD_TYPE#IGNORE}.
     */
    @Override
    public boolean supports(T newValue, T oldValue, DefaultValidatorConfig.METHOD_TYPE methodType) {
        if(methodType == DefaultValidatorConfig.METHOD_TYPE.IGNORE){
            return false;
        }
        return true;
    }
}
