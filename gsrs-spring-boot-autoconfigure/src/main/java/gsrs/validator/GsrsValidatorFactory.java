package gsrs.validator;

import ix.ginas.utils.validation.ValidatorFactory;

public interface GsrsValidatorFactory {
    ValidatorFactory newFactory(String context);
}
