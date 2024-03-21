package gsrs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties("ix.api")
@Data
public class RegisteredFunctionProperties {

    private RegisteredFunctions registeredFunctions;

    @Data
    public static class RegisteredFunctions {
        private Map<String, Map<String, Object>> list;
    }
}
