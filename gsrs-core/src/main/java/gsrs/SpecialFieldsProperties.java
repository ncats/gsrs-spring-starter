package gsrs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("ix.core")
public class SpecialFieldsProperties {

    private List<Map<String, Object>> exactsearchfields = new ArrayList<>();

    public List<Map<String, Object>> getExactsearchfields() {
        
        return exactsearchfields;
    }

    public void setExactsearchfields(List<Map<String, Object>> exactsearchfields) {
        if(exactsearchfields!=null) {
            this.exactsearchfields = exactsearchfields;
        }
       
    }


}