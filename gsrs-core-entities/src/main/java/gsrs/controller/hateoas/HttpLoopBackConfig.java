package gsrs.controller.hateoas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("gsrs.loopback")
public class HttpLoopBackConfig {
    
    // TODO: we may be able to simplify this to one
    // config parameter, much like application.host
    // it should instead be gsrs.loopback.application.host
    // as that is the analogous concept.
    //
    // It's also probably possible to detect the most
    // likely loopback based on requests. This is
    // especially likely when using a microservice
    // architecture, so some simple "useSelf"
    // loopback adapter would be useful
    private String protocol;
    private String hostname;
    private int port;
    
    
    private List<Map<String, Object>> requests = new ArrayList<>();

    public String getProtocol() {
        return protocol;
    }
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    public String getHostname() {
        return hostname;
    }
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public List<Map<String, Object>> getRequests() {
        return requests;
    }
    public void setRequests(List<Map<String, Object>> requests) {
        this.requests = requests;
    }
    
    
}
