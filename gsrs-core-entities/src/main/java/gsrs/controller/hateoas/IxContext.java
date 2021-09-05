package gsrs.controller.hateoas;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import gsrs.springUtils.StaticContextAccessor;
import lombok.Data;

@Configuration
@Data
public class IxContext {
    
    @Value("${application.host:#{null}}")
    private String host;
    

    /**
     * Attempts to adapt the received URI (likely as seen by the server)
     * into the URI expected by a client (perhaps through proxies, etc) 
     * @param uri
     * @return
     */
    // TODO: Major link refactoring is required to fix special cases of contexts in
    // web containers, extra prefix contexts, etc
    public URI getEffectiveAdaptedURI(URI uri) {
        if(host==null) return uri;
        String uhost = requestHostAndPort(uri);
        String rhost = getEffectiveHostURI();
        
        return URI.create(uri.toString().replace(uhost,rhost));
    }
    
    
    public URI getEffectiveAdaptedURI() {
        return getEffectiveAdaptedURI(ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri());
    }
    
    public URI getEffectiveAdaptedURI(HttpServletRequest req) {
        String url="";
        String queryString = req.getQueryString();
        if(queryString ==null || queryString.isEmpty()){
            url= req.getRequestURL().toString();
        }else {
            url= req.getRequestURL().toString() + "?" + req.getQueryString();
        }
        return getEffectiveAdaptedURI(URI.create(url));
    }
    
    private String requestHostAndPort(URI uri) {
        StringBuilder apiBuilder = new StringBuilder();
        String host = uri.getHost();
        int port = uri.getPort();
        String scheme = uri.getScheme();
        if(scheme !=null){
            apiBuilder.append(scheme+"://");
        }
        apiBuilder.append(host==null?"localhost": host);
        if(port >=0){
            apiBuilder.append(":"+port);
        }
        return apiBuilder.toString();
    }
    
    public String getEffectiveHostURI() {
        String configHostAndPort = this.getHost();
        StringBuilder apiBuilder = new StringBuilder();
        
        if(configHostAndPort==null) {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();

            URI uri = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
            apiBuilder.append(requestHostAndPort(uri));
        }else {
            apiBuilder.append(configHostAndPort);
        }
        return apiBuilder.toString();
    }
    
}
