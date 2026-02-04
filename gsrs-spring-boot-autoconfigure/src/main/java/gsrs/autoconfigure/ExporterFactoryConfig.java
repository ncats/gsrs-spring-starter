package gsrs.autoconfigure;

import gsrs.util.ExtensionConfig;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExporterFactoryConfig implements ExtensionConfig {
    private Class exporterFactoryClass;
    private String parentKey;
    private Double order;
    private boolean disabled = false;
    private Map<String, Object> parameters;
    private List<String> enablingPrivileges;
}
