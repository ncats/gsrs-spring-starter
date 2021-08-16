package gsrs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties("ix.core")
public class SpecialFieldsProperties {

    private List<Map<String, Object>> exactsearchfields;

    public List<Map<String, Object>> getExactsearchfields() {
        return exactsearchfields;
    }

    public void setExactsearchfields(List<Map<String, Object>> exactsearchfields) {
        this.exactsearchfields = exactsearchfields;
    }


}