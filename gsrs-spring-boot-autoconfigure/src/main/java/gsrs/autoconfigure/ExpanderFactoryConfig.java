package gsrs.autoconfigure;

import lombok.Data;

import java.util.Map;

@Data
public class ExpanderFactoryConfig {
    private Class expanderFactory;
    private Map<String,Object> parameters;
}
