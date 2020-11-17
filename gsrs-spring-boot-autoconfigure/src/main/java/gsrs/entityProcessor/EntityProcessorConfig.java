package gsrs.entityProcessor;

import com.fasterxml.jackson.annotation.JsonProperty;
import ix.core.EntityProcessor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Constructor;
import java.util.Map;
@Data
public class EntityProcessorConfig {
    @JsonProperty("class")
    private Class entityClassName;
    @JsonProperty("processor")
    private Class processor;
    private Map with;



    public EntityProcessor createNewEntityProcessorInstance() throws Exception{

        if(with!=null){
            Constructor c=processor.getConstructor(Map.class);
            return (EntityProcessor) c.newInstance(with);
        }else{
            return (EntityProcessor) processor.newInstance();
        }

    }
}
