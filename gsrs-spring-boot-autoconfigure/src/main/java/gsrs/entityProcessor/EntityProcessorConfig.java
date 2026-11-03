package gsrs.entityProcessor;

import gsrs.util.ExtensionConfig;
import ix.core.EntityProcessor;
import lombok.Data;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Constructor;
import java.util.Map;
@Data
public class EntityProcessorConfig implements ExtensionConfig {
    private Class entityClassName;
    private Class processor;
    private String parentKey;
    private Double order;
    private boolean disabled = false;

    /**
     * Legacy method of passing parameters to a constructor with a Map parameter
     */

    private Map with;


    private Map<String, Object> parameters;

    public EntityProcessor createNewEntityProcessorInstance() throws Exception{

        JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();
        if(with!=null){
            Constructor c=processor.getConstructor(Map.class);
            return (EntityProcessor) c.newInstance(with);
        }else if(parameters !=null){
            return (EntityProcessor) mapper.convertValue(parameters, processor);
        }else{
            return (EntityProcessor) processor.newInstance();
        }

    }



}
