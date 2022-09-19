package gsrs.autoconfigure;

import lombok.Data;

import java.util.Map;

@Data
public class ExporterFactoryConf {
    private Class exporterClass;
    private Map<String, Object> parameters;
}
