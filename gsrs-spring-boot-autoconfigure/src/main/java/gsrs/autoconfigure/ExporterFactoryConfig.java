package gsrs.autoconfigure;

import lombok.Data;

import java.util.Map;

@Data
public class ExporterFactoryConfig {
    private Class exporterFactoryClass;
    private String key;
    private Double order;
    private boolean disabled = false;
    private Map<String, Object> parameters;
}
