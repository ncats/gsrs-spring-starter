package gsrs.validator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
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
    private Class newObjClass;

    private METHOD_TYPE methodType;


    @Override
    public ValidatorPlugin newValidatorPlugin(ObjectMapper mapper, ClassLoader classLoader) throws ClassNotFoundException {

        if(parameters ==null){
            return (ValidatorPlugin) mapper.convertValue(Collections.emptyMap(), validatorClass);

        }
        return (ValidatorPlugin) mapper.convertValue(parameters, validatorClass);

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
