package gsrs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties("ix.api")

public class RegisteredFunctionProperties {

    private List<Map<String, Object>> registeredfunctions;


    public List<Map<String, Object>> getRegisteredfunctions() {
        return registeredfunctions;
    }

    public void setRegisteredfunctions(List<Map<String, Object>> registeredfunctions) {
        this.registeredfunctions = registeredfunctions;
    }


}
