package ix.ginas.utils.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.springUtils.AutowireHelper;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by katzelda on 5/7/18.
 */

public class ValidatorFactory {


    private final Map<ValidatorPlugin, ValidatorConfig> plugins = new LinkedHashMap<>();


    public ValidatorFactory(List<? extends ValidatorConfig> configs, ObjectMapper mapper){
       for(ValidatorConfig conf : configs){
           try {

               ValidatorPlugin p  = conf.newValidatorPlugin(mapper, AutowireHelper.getInstance().getClassLoader());
               AutowireHelper.getInstance().autowire(p);
               plugins.put(p, conf);
           } catch (Exception e) {
               e.printStackTrace();
           }

       }
    }


    public <T> Validator<T> createValidatorFor(T newValue, T oldValue, DefaultValidatorConfig.METHOD_TYPE methodType){
        return plugins.entrySet().stream()
                .filter( e-> e.getValue().meetsFilterCriteria(newValue, methodType) && e.getKey().supports(newValue, oldValue, methodType))
                .map(e -> (Validator<T>) e.getKey())
//                .peek(v -> System.out.println("running validator : " + v))
                .reduce(Validator.emptyValid(), Validator::combine);
    }




}
