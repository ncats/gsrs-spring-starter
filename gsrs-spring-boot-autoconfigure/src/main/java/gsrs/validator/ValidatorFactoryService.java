package gsrs.validator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import ix.ginas.utils.validation.ValidatorFactory;
import ix.ginas.utils.validation.ValidatorPlugin;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;


import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by katzelda on 5/7/18.
 */
@Service
public class ValidatorFactoryService {
    @Data
    public static class ValidatorConfig{


        private String validatorClass;
        /**
         * Additional parameters to initialize in your instance returned by
         * {@link #getValidatorClass()}.
         */
        private Map<String, Object> parameters;
        private String newObjClass;
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
        @JsonIgnore
        private final CachedSupplier<Class> validatorSupplier = CachedSupplier.runOnce(()->loadClass(validatorClass));
        @JsonIgnore
        private final CachedSupplier<Class> newObjSupplier = CachedSupplier.runOnce(()->loadClass(newObjClass));

        private Class<?> loadClass(String className) {
            try {
                return ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
            } catch (ClassNotFoundException e) {
               throw new IllegalStateException(e);
            }
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public ValidatorConfig setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }


        public ValidatorPlugin newValidatorPlugin(ObjectMapper mapper)  {

            if(parameters ==null){
                return (ValidatorPlugin) mapper.convertValue(Collections.emptyMap(), validatorSupplier.get());

            }
            return (ValidatorPlugin) mapper.convertValue(parameters, validatorSupplier.get());

        }
        public final  <T> boolean meetsFilterCriteria(T obj, ValidatorFactoryService.ValidatorConfig.METHOD_TYPE methodType){
            if(!newObjSupplier.get().isAssignableFrom(obj.getClass())){
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



    public ValidatorFactory newFactory(String context, ObjectMapper mapper) {
        List<ValidatorConfig> configs = validatorFactoryConfiguration.getValidatorConfigByContext(context);
        return new ValidatorFactory(configs, mapper);
    }

}
