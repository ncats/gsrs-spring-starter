package gsrs.autoconfigure;

import lombok.Data;

import java.util.Map;

@Data
public class ScrubberFactoryConfig {
    private Class scrubberFactoryClass;
    private Map<String,Object> parameters;
}
