package gsrs.api;

import lombok.Data;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.LinkedHashMap;
import java.util.Map;


@Data
public class AbstractGsrsRestApiConfiguration {

    private String baseURL;

    private Map<String,String> headers = new LinkedHashMap<>();

    public void configure(RestTemplateBuilder restTemplateBuilder){
        for(Map.Entry<String,String> entry: headers.entrySet()){
            restTemplateBuilder.defaultHeader(entry.getKey(), entry.getValue());
        }
    }

}
