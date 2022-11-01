package gsrs.autoconfigure;

import lombok.Data;

import java.util.Map;

/*
This class exists to map config file data into a set of scrubbers and parameters
 */
@Data
public class ScrubberFactoryConfig {
    private Class scrubberFactoryClass;
    private Map<String,Object> parameters;
}
