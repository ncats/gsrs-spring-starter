package gsrs.controller.hateoas;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import gov.nih.ncats.common.Tuple;
import gsrs.springUtils.GsrsSpringUtils;
import lombok.Builder;

@Builder
public class HttpRequestHolder {
//    @Builder.Default
//    private HttpEntity<String> entity = new HttpEntity<>("body");

    @Builder.Default
    private Map<String,List<String>> headers = new LinkedHashMap<>();

    @Builder.Default
    private HttpMethod method = HttpMethod.GET;

    private String url;

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public HttpMethod getMethod() {
        return method;
    }
    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public void addHeader(String key, String v) {
        if(headers==null) {
            headers= new LinkedHashMap<>();
        }
       headers.computeIfAbsent(key, kk-> new ArrayList<>())
              .add(v);
    }

    public ResponseEntity<String> execute(RestTemplate restTemplate) {
        String curl=url;
        // Tyler Peryea: This section is unfortunately necessary as
        // the restTemplate itself considers all URLs provided to be not-yet percent encoded
        // so, in a rather silly turn of events we need to UNencode the URL before sending it
        // to restTemplate to be re-encoded.
        try {
            curl = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } // java.net class
        HttpHeaders httpheaders = new HttpHeaders();
        headers.forEach((k,v)->{
            httpheaders.addAll(k, v);
        });
        HttpEntity<String> entity = new HttpEntity<>("body",httpheaders);
        
        return restTemplate.exchange(curl, method, entity, String.class);
    }
    
    public static HttpRequestHolder fromRequest(HttpServletRequest req) {
        HttpRequestHolder holder=HttpRequestHolder.builder()
        .url(req.getRequestURL() + Optional.ofNullable(req.getQueryString()).map(qq->"?"+qq).orElse(""))
        .method(HttpMethod.valueOf(req.getMethod()))
        .build();
        GsrsSpringUtils.toHeadersMap(req).forEach((k,v)->{
            for(String v2:v) {
                holder.addHeader(k, v2);
            }
        });
        
        return holder;
    }
    public Map<String, List<String>> getHeaders() {
        return headers;
    }
    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }
    
    
    
    
}
