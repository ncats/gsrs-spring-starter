package ix.ginas.utils.validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gsrs.springUtils.AutowireHelper;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.interfaces.GsrsJsonMapper;
import ix.core.validator.Validator;
import ix.core.validator.ValidatorCategory;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by katzelda on 5/7/18.
 */

@Slf4j
public class ValidatorFactory {


    private final Map<ValidatorPlugin, ValidatorConfig> plugins = new LinkedHashMap<>();


    public ValidatorFactory(List<? extends ValidatorConfig> configs, GsrsJsonMapper mapper){
       for(ValidatorConfig conf : configs){
           try {

               ValidatorPlugin p  = conf.newValidatorPlugin(mapper, AutowireHelper.getInstance().getClassLoader());
               p = AutowireHelper.getInstance().autowireAndProxy(p);
               //TODO initialize throws IllegalStateException should we catch it and report it somewhere?
               p.initialize();
               plugins.put(p, conf);
           } catch (Exception e) {
               log.warn("Exception during validator plugin init: ", e);
           }

       }
    }


    public <T> Validator<T> createValidatorFor(T newValue, T oldValue, DefaultValidatorConfig.METHOD_TYPE methodType, ValidatorCategory category){
        return plugins.entrySet().stream()
                .filter( e-> e.getValue().meetsFilterCriteria(newValue, methodType))
                .filter( e-> e.getKey().supports(newValue, oldValue, methodType))
                .filter( e-> e.getKey().supportsCategory(newValue, oldValue, category))
                //temporarily comment out the next line so we can use the 'timed' alternative while investigating the system.
                //TODO: revert!
                //.map(e -> (Validator<T>) e.getKey())
                .map(e -> timed(
                        (Validator<T>) e.getKey(),
                        e.getValue()))
                .reduce(Validator.emptyValid(), Validator::combine);
    }


    private <T> Validator<T> timed(
            Validator<T> validator,
            ValidatorConfig config) {

        String validatorName =
                config.getValidatorClass() == null
                        ? validator.getClass().getName()
                        : config.getValidatorClass().getName();

        return (newValue, oldValue, callback) -> {
            long start = System.nanoTime();

            try {
                validator.validate(newValue, oldValue, callback);
            } finally {
                double elapsedMillis =
                        (System.nanoTime() - start) / 1_000_000.0;

                log.info(
                        "Validator timing: validator={}, elapsedMs={}",
                        validatorName,
                        String.format("%.3f", elapsedMillis));
            }
        };
    }


}
