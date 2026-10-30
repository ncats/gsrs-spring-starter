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

    /**
     * Describes how validators are selected for an operation. Most operations
     * use the same method type for configuration filtering and plugin behavior.
     * Staging is the exception: it has its own configuration while retaining
     * the established batch behavior of validator plugins.
     */
    public record ValidationContext(
            DefaultValidatorConfig.METHOD_TYPE configurationMethodType,
            DefaultValidatorConfig.METHOD_TYPE validatorMethodType) {

        public static ValidationContext forMethodType(DefaultValidatorConfig.METHOD_TYPE methodType) {
            return new ValidationContext(methodType, methodType);
        }

        public static ValidationContext stagingWithBatchBehavior() {
            return new ValidationContext(
                    DefaultValidatorConfig.METHOD_TYPE.STAGING,
                    DefaultValidatorConfig.METHOD_TYPE.BATCH);
        }
    }


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
        return createValidatorFor(newValue, oldValue, ValidationContext.forMethodType(methodType), category);
    }

    /**
     * Creates a validator while allowing configuration filtering to use a
     * different operation context from the one exposed to validator plugins.
     * This is useful for staging imports, which have staging-specific
     * configuration but retain the established batch-validation behavior.
     */
    public <T> Validator<T> createValidatorFor(T newValue, T oldValue,
                                                ValidationContext validationContext,
                                                ValidatorCategory category){
        return plugins.entrySet().stream()
                .filter( e-> e.getValue().meetsFilterCriteria(
                        newValue, validationContext.configurationMethodType()))
                .filter( e-> e.getKey().supports(
                        newValue, oldValue, validationContext.validatorMethodType()))
                .filter( e-> e.getKey().supportsCategory(newValue, oldValue, category))
                 .map(e -> (Validator<T>) e.getKey())
                .reduce(Validator.emptyValid(), Validator::combine);
    }


}
