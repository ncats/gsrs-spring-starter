package gsrs.controller.hateoas;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.common.util.Holder;



@Component
public class LoopbackWebRequestHelper implements ApplicationListener<ContextRefreshedEvent>{

    @Autowired
    private HttpLoopBackConfig httpLoopBackConfig;
    

    private Map<String, RequestAdapter> adapterMapByContext;
    private RequestAdapter defaultAdapter;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if(httpLoopBackConfig!=null) {
            Holder<RequestAdapter> defaultHolder = Holder.hold(null);
            ObjectMapper mapper = new ObjectMapper();
            adapterMapByContext = httpLoopBackConfig.getRequests().stream()
                    .map(m-> mapper.convertValue(m, WebRequestConfig.class))
                    .filter(m->{
                        if(m.isDefault()){
                            defaultHolder.set( getRequestAdapter(mapper, m));
                            return false;
                        }else{
                            return true;
                        }
                    })
                    .collect(Collectors.toMap(WebRequestConfig::getContext, c-> getRequestAdapter(mapper, c)));
            defaultAdapter = defaultHolder.get();
        }
    }

    public HttpRequestHolder createNewLoopbackRequestFromCurrentRequest(HttpRequestHolder holder){
        return createNewLoopbackRequestFromCurrentRequest(holder, null);
    }
    
    public HttpRequestHolder createNewLoopbackRequestFromCurrentRequest(HttpRequestHolder holder, String context){
        HttpServletRequest request = 
                ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes())
                        .getRequest();
        HttpRequestHolder sourceHold = HttpRequestHolder.fromRequest(request);
        return adapt(holder, sourceHold, adapterMapByContext.get(context));
    }
    
    
    public HttpRequestHolder createNewLoopbackRequestFrom(HttpRequestHolder holder, HttpRequestHolder currentRequest) {
        return adapt(holder,currentRequest, null);
    }
    
    public HttpRequestHolder createNewLoopbackRequestFrom(HttpRequestHolder holder, HttpRequestHolder currentRequest, String context){
        RequestAdapter adapter = adapterMapByContext.get(context);
        return adapt(holder, currentRequest, adapter);
    }
  

    private HttpRequestHolder adapt(HttpRequestHolder holdersrc, HttpRequestHolder currentRequest, RequestAdapter adapter) {
        String transformedURL;
        try {
            URI urlObj = new URL(holdersrc.getUrl()).toURI();


            transformedURL = new URI(httpLoopBackConfig.getProtocol(), 
                                     httpLoopBackConfig.getHostname() +":"+ 
                                     httpLoopBackConfig.getPort(),
                                                urlObj.getPath(),     //TODO: TP 08-15-2021 I'm not so sure about this ... the local loopback could
                                                                      //be different than the path of the built query. Care needs
                                                                      //to be taken here
                                                urlObj.getQuery(), 
                                                urlObj.getFragment())
                    .toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        HttpRequestHolder requestHolder = HttpRequestHolder.builder()
                .url(transformedURL)
                .headers(holdersrc.getHeaders())
                .method(holdersrc.getMethod())
                .build();
        if(adapter !=null) {
            return adapter.adaptHolder(requestHolder, currentRequest);
        }
        return defaultAdapter.adaptHolder(requestHolder, currentRequest);
    }


    private RequestAdapter getRequestAdapter(ObjectMapper mapper, WebRequestConfig c) {
        Map<String, Object> map;
        if(c.getParameters() ==null){
           map = Collections.emptyMap();
        }else{
            map = c.getParameters();
        }
        try {
            
            //This deals with the unfortunate tendency of the HOCON parser
            //for spring boot to deserialize JSON arrays into Map<Integer,Object>
            //instead of List<Object> or equivalent. This needs a deeper solution
            //perhaps involving jackson or the HOCON property parser itself
            map = map.entrySet().stream()
            .map(Tuple::of)
            .map(Tuple.vmap(v->{
                if(v instanceof LinkedHashMap) {
                    LinkedHashMap lhm = (LinkedHashMap)v;
                    if(lhm.keySet().stream()
                    .allMatch(kk->{
                        try {
                            Integer ind = Integer.parseInt(kk+"");
                            if(ind!=null)return true;
                        }catch(Exception e) {
                        }
                        return false;
                    })) {
                        return lhm.values();
                    }
                }
                return v;
                                
            }))
            .collect(Tuple.toMap());
            
            
            return (RequestAdapter) mapper.convertValue(map, LoopbackWebRequestHelper.class.getClassLoader().loadClass(c.getClassname()));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }


    public interface RequestAdapter{
        HttpRequestHolder adaptHolder(HttpRequestHolder holder, HttpRequestHolder sourceRequest);
    }
    public static class WebRequestConfig{
        public String context;
        public String classname;
        public boolean isDefault;
        public Map<String, Object> parameters;

        public boolean isDefault() {
            return isDefault;
        }

        public void setDefault(boolean aDefault) {
            isDefault = aDefault;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public String getClassname() {
            return classname;
        }

        public void setClassname(String classname) {
            this.classname = classname;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    public static class AuthHeaderRequestAdapter implements RequestAdapter{

        private Set<String> authHeaders = new HashSet<>();

        public Set<String> getAuthHeaders() {
            return authHeaders;
        }

        public void setAuthHeaders(Set<String> authHeaders) {
            this.authHeaders = authHeaders;
        }
        
        @Override
        public HttpRequestHolder adaptHolder(HttpRequestHolder holder, HttpRequestHolder sourceRequest) {
            if(authHeaders.isEmpty()){
                return holder;
            }
            Map<String, List<String>> headers = sourceRequest.getHeaders();
            for(String key : authHeaders){
                List<String> value = headers.get(key);
                if(value !=null && value.size() > 0){
                    for(String v : value) {
                        holder.addHeader(key, v);
                    }
                }
            }
            return holder;
        }
    }
   
}
