package gsrs.util;

import lombok.Data;

import java.util.Map;

@Data
public class RegisteredFunctionConfig implements ExtensionConfig {
    private Class registeredFunctionClass;
    private String parentKey;
    private Double order;
    private boolean disabled = false;
    private Map<String, Object> parameters;
}
