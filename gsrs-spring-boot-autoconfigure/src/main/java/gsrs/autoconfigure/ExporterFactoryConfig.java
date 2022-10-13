package gsrs.autoconfigure;

import lombok.Data;

import java.util.Map;

@Data
public class ExporterFactoryConfig {
    private Class exporterFactoryClass;
    private Map<String, Object> parameters;
}
