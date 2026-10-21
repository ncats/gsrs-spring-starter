package gsrs.validator;

import com.fasterxml.jackson.annotation.*;
import ix.core.interfaces.GsrsJsonMapper;
import ix.core.util.InheritanceTypeIdResolver;
import ix.ginas.utils.validation.ValidatorPlugin;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@InheritanceTypeIdResolver.DefaultInstance
public class DefaultValidatorConfig implements ValidatorConfig {

    private Class validatorClass;
    /**
     * Additional parameters to initialize in your instance returned by
     * {@link #getValidatorClass()}.
     */
    private Map<String, Object> parameters;
    /**
     * Catch all for additional JSON properties found will be assumed to be
     * parameters to pass to the validator class similar to {@link #parameters}.
     * if both parameters and unknown parameters are both set then parameters takes
     * precedence.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> unknownParameters = new ConcurrentHashMap<>();

    private Class newObjClass;
    private String parentKey;
    private Double order;
    private boolean disabled = false;



    private METHOD_TYPE methodType;

    /**
     * Validation operations for which this validator should not run. This
     * complements methodType, which limits a validator to one operation.
     */
    private Set<METHOD_TYPE> excludedMethodTypes = Collections.emptySet();

    /**
     * Spring's configuration-properties binder represents a collection nested
     * below a {@code Map<String, Object>} as an indexed map (for example,
     * {@code {0=STAGING}}). Accept that representation as well as the normal
     * JSON collection representation used by Jackson.
     */
    @JsonSetter("excludedMethodTypes")
    public void setExcludedMethodTypes(Object excludedMethodTypes) {
        if (excludedMethodTypes == null) {
            this.excludedMethodTypes = Collections.emptySet();
            return;
        }

        Collection<?> values = excludedMethodTypes instanceof Map<?, ?> map
                ? map.values()
                : excludedMethodTypes instanceof Collection<?> collection
                        ? collection
                        : Collections.singleton(excludedMethodTypes);

        Set<METHOD_TYPE> methodTypes = new LinkedHashSet<>();
        for (Object value : values) {
            methodTypes.add(value instanceof METHOD_TYPE methodType
                    ? methodType
                    : METHOD_TYPE.valueOf(String.valueOf(value)));
        }
        this.excludedMethodTypes = methodTypes;
    }

    /**
     * Catch all for additional JSON properties found will be assumed to be
     * parameters to pass to the validator class similar to {@link #parameters}.
     * If both parameters and unknown parameters are both set then parameters takes
     * precedence.
     * @param propertyKey
     * @param value
     */
    @JsonAnySetter
    public void addUnknownParameter(String propertyKey, Object value){
        unknownParameters.put(propertyKey, value);
    }

    @Override
    public ValidatorPlugin newValidatorPlugin(GsrsJsonMapper mapper, ClassLoader classLoader) throws ClassNotFoundException {

        if(parameters !=null && !parameters.isEmpty()){
            return (ValidatorPlugin) mapper.convertValue(parameters, validatorClass);
        }
        if(unknownParameters !=null && !unknownParameters.isEmpty()){
            return (ValidatorPlugin) mapper.convertValue(unknownParameters, validatorClass);

        }
        return (ValidatorPlugin) mapper.convertValue(Collections.emptyMap(), validatorClass);


    }
    @Override
    public final <T> boolean meetsFilterCriteria(T obj, METHOD_TYPE methodType){
        if(!newObjClass.isAssignableFrom(obj.getClass())){
            return false;
        }
        if(excludedMethodTypes != null && excludedMethodTypes.contains(methodType)){
            return false;
        }
        if(this.methodType !=null && this.methodType != methodType){
            return false;
        }

        return meetsFilterCriteria(obj);
    }

    protected <T> boolean meetsFilterCriteria(T obj){
        return true;
    }

}
