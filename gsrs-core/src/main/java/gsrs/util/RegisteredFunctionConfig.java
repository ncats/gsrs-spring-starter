package gsrs.util;

import lombok.Data;

import java.util.Map;

@Data
public class RegisteredFunctionConfig {
    private Class registeredFunctionClass;
    private String key;
    private Double order;
    private boolean disabled = false;
    private Map<String, Object> parameters;
}
