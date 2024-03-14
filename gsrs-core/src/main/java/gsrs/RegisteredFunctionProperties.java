package gsrs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties("ix.api")

public class RegisteredFunctionProperties {

    private Map<String, Map<String, Object>> registeredfunctions;


    public Map<String, Map<String, Object>> getRegisteredfunctions() {
        return registeredfunctions;
    }

    public void setRegisteredfunctions(Map<String, Map<String, Object>> registeredfunctions) {
        this.registeredfunctions = registeredfunctions;
    }


}
