package gsrs.validator;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import ix.core.util.InheritanceTypeIdResolver;
import ix.ginas.utils.validation.ValidatorPlugin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    public ValidatorPlugin newValidatorPlugin(ObjectMapper mapper, ClassLoader classLoader) throws ClassNotFoundException {

        if(parameters !=null && !parameters.isEmpty()){
            return (ValidatorPlugin) mapper.convertValue(parameters, validatorClass);
        }
        if(unknownParameters !=null && !unknownParameters.isEmpty()){
            return (ValidatorPlugin) mapper.convertValue(unknownParameters, validatorClass);

        }
        return (ValidatorPlugin) mapper.convertValue(Collections.emptyMap(), validatorClass);


    }
    @Override
    public final  <T> boolean meetsFilterCriteria(T obj, METHOD_TYPE methodType){
        if(!newObjClass.isAssignableFrom(obj.getClass())){
            return false;
        }
//            if(obj instanceof Substance){
//                Substance s = (Substance) obj;
//                if(substanceClass !=null && substanceClass != s.substanceClass){
//                    return false;
//                }
//                if(type !=null && type != s.definitionType){
//                    return false;
//                }
//
//            }
        if(methodType !=null && methodType != methodType){
            return false;
        }

        return meetsFilterCriteria(obj);
    }
    protected <T> boolean meetsFilterCriteria(T obj){
        return true;
    }

}
