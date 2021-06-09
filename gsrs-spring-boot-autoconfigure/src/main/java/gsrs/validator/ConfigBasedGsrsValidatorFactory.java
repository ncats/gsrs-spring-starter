package gsrs.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.GsrsFactoryConfiguration;
import ix.ginas.utils.validation.ValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;

/**
 * a {@link GsrsValidatorFactory} that uses the conf
 * to find  the validators to use.
 */
public class ConfigBasedGsrsValidatorFactory implements GsrsValidatorFactory {




    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;



    @Override
    public ValidatorFactory newFactory(String context) {
        List<ValidatorConfig> configs = gsrsFactoryConfiguration.getValidatorConfigByContext(context);
        return new ValidatorFactory(configs, new ObjectMapper());
    }

}
