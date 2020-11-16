package gsrs.entityProcessor;

import lombok.Data;

import java.util.Map;
@Data
public class EntityProcessorConfig {
        private Class entityClassName;
        private Class processorClassName;
        private Map with;
}
