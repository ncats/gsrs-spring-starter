package gsrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.entityProcessor.EntityProcessorConfig;
import gsrs.validator.ValidatorConfig;
import gsrs.validator.ValidatorConfigList;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
@Component
@ConfigurationProperties("gsrs")
@Data
public class GsrsFactoryConfiguration {

    private Map<String, List<Map<String,Object>>> validators;
    private List<EntityProcessorConfig> entityProcessors;

    private boolean createUnknownUsers= false;

    public List<EntityProcessorConfig> getEntityProcessors(){
        if(entityProcessors ==null){
            //nothing set
            return Collections.emptyList();
        }
        return new ArrayList<>(entityProcessors);
    }



    public List<ValidatorConfig> getValidatorConfigByContext(String context){
        if(validators ==null){
            //nothing set
            return Collections.emptyList();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Map<String,Object>> list = validators.get(context);
            if(list==null || list.isEmpty()){
                return Collections.emptyList();
            }
            List<ValidatorConfig> configs = new ArrayList<>();
            for (Map<String,Object> n : list) {
                configs.add(mapper.convertValue(n, ValidatorConfig.class));
            }
            return configs;
        }catch(Throwable t){
            throw t;
        }
//        ValidatorConfigList list = (ValidatorConfigList) validators.get(context);
//        if(list ==null){
//            return Collections.emptyList();
//        }
//        return list.getConfigList();
    }
}
