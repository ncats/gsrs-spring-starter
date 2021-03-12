package gsrs.validator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import ix.core.util.InheritanceTypeIdResolver;
import ix.ginas.utils.validation.ValidatorPlugin;

import java.util.Map;
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "configClass", defaultImpl = DefaultValidatorConfig.class)
@JsonTypeIdResolver(InheritanceTypeIdResolver.class)
public interface ValidatorConfig {
    Map<String, Object> getParameters();

    void setParameters(Map<String, Object> parameters);

    ValidatorPlugin newValidatorPlugin(ObjectMapper mapper, ClassLoader classLoader) throws ClassNotFoundException;

    <T> boolean meetsFilterCriteria(T obj, METHOD_TYPE methodType);

    Class getValidatorClass();

    Class getNewObjClass();

    METHOD_TYPE getMethodType();

    void setValidatorClass(Class validatorClass);

    void setNewObjClass(Class newObjClass);

    void setMethodType(METHOD_TYPE methodType);

    enum METHOD_TYPE{

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
}
