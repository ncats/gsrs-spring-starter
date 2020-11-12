package gsrs.validator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import ix.ginas.utils.validation.ValidatorPlugin;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

@Data
public class ValidatorConfig {


    private Class validatorClass;
    /**
     * Additional parameters to initialize in your instance returned by
     * {@link #getValidatorClass()}.
     */
    private Map<String, Object> parameters;
    private Class newObjClass;
//        private Substance.SubstanceDefinitionType type;
    private METHOD_TYPE methodType;

//        private Substance.SubstanceClass substanceClass;
    public enum METHOD_TYPE{

        CREATE,
        UPDATE,
        APPROVE,
        BATCH,
        IGNORE

        ;

        @JsonValue
        public String jsonValue(){
            return name();
        }
    }



    public Map<String, Object> getParameters() {
        return parameters;
    }

    public ValidatorConfig setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        return this;
    }


    public ValidatorPlugin newValidatorPlugin(ObjectMapper mapper, ClassLoader classLoader) throws ClassNotFoundException {

        if(parameters ==null){
            return (ValidatorPlugin) mapper.convertValue(Collections.emptyMap(), validatorClass);

        }
        return (ValidatorPlugin) mapper.convertValue(parameters, validatorClass);

    }
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
