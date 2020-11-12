package gsrs.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import ix.ginas.utils.validation.ValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;


import java.util.List;

/**
 * Created by katzelda on 5/7/18.
 */
@Service
public class GsrsValidatorFactory {


    @Autowired
    private ValidatorFactoryConfiguration validatorFactoryConfiguration;


//    @PostConstruct
//    public void onStart(Application app) {
//        this.instance = this;
//        List<?> list = app.configuration().getList("substance.validators");
//        if(list == null){
//            throw new IllegalStateException("substance validators must be specified in the config");
//        }
//        ObjectMapper mapper = new ObjectMapper();
//        configs = list.stream()
//                .map(m-> mapper.convertValue(m, ValidatorConfig.class))
//                .collect(Collectors.toList());
//    }



    public ValidatorFactory newFactory(String context) {
        List<ValidatorConfig> configs = validatorFactoryConfiguration.getValidatorConfigByContext(context);
        return new ValidatorFactory(configs, new ObjectMapper());
    }

}
