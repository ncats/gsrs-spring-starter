package gsrs.api;

import lombok.Data;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.LinkedHashMap;
import java.util.Map;


@Data
public class AbstractGsrsRestApiConfiguration {

    private String baseURL;

    private Map<String,String> headers = new LinkedHashMap<>();

    /**
     * Create a new {@link RestTemplateBuilder} using the configuration settings.
     * @return
     */
    public RestTemplateBuilder createNewRestTemplateBuilder(){
        return addDefaultHeaders(new RestTemplateBuilder());
    }
    @Deprecated
    public RestTemplateBuilder configure(RestTemplateBuilder restTemplateBuilder){
        return addDefaultHeaders(restTemplateBuilder);
    }

    private RestTemplateBuilder addDefaultHeaders(RestTemplateBuilder restTemplateBuilder) {
        for(Map.Entry<String,String> entry: headers.entrySet()){
            restTemplateBuilder = restTemplateBuilder.defaultHeader(entry.getKey(), entry.getValue());
        }
        return restTemplateBuilder;
    }

}
