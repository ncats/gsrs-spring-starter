package gsrs.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gsrs.api.internal.WithoutContentPagedResult;
import gsrs.controller.GsrsEntityController;
import ix.core.validator.ValidationResponse;
import lombok.Builder;
import lombok.Data;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.Link;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;

public abstract class GsrsEntityRestTemplate<T, I> {

    private final RestTemplate restTemplate;

    private ObjectMapper mapper;
    private String prefix;

    public GsrsEntityRestTemplate(RestTemplateBuilder restTemplateBuilder, String baseUrl, String context, ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper);

        StringBuilder builder = new StringBuilder(baseUrl);

        builder.append("/api/v1/").append(context);
        if(context.endsWith("/")){
            builder.setLength(builder.length()-1);
        }
        this.prefix = builder.toString();
        this.restTemplate = restTemplateBuilder.rootUri(baseUrl)
                .errorHandler(RestTemplateResponseErrorHandler.INSTANCE)
                .build();
    }
    public GsrsEntityRestTemplate(RestTemplateBuilder restTemplateBuilder, String baseUrl, String context) {
        this(restTemplateBuilder, baseUrl, context, new ObjectMapper());
    }

    protected ObjectMapper getObjectMapper(){
        return mapper;
    }

    protected abstract I getIdFrom(T dto);




    public long count() throws IOException {
        ResponseEntity<String> response = doGet("/@count",String.class);

        if(response.getStatusCode().is2xxSuccessful()) {
            return Long.parseLong(response.getBody());
        }
        throw new IOException("error getting count: " + ((HttpStatus) response.getStatusCode()).getReasonPhrase());
    }

    public <S extends T> Optional<S> findByResolvedId(String anyKindOfId) throws IOException{
        ResponseEntity<String> response = doGet("("+anyKindOfId + ")",String.class);
        if(response.getStatusCode() == HttpStatus.NOT_FOUND){
            return Optional.empty();
        }else if(response.getStatusCode().is2xxSuccessful()){
            JsonNode node = mapper.readTree(response.getBody());
            return Optional.ofNullable(parseFromJson(node));
        }else{
            throw new IOException("Unexpected server response:" + response.getStatusCode());
        }


    }
    public <S extends T> Optional<S> findById(I id) throws IOException {
        return findByResolvedId(id.toString());
    }
    public boolean existsById(I id) throws IOException {
        ResponseEntity<String> response = doGet("("+id + ")", "key",String.class);
        if(response.getStatusCode() == HttpStatus.NOT_FOUND){
            return false;
        }else if(response.getStatusCode().is2xxSuccessful()){
            return true;
        }else{
            throw new IOException("Unexpected server response: " + response.getStatusCode());
        }
    }

    public ExistsCheckResult exists(String... anyKindOfIdString) throws IOException{
       List<String> list = new ArrayList<>(anyKindOfIdString.length);
       for(String s : anyKindOfIdString){
           if(s !=null){
               list.add(s);
           }
       }
       return restTemplate.postForObject(prefix+"/@exists", list, ExistsCheckResult.class);
    }

    public <S extends T> Optional<PagedResult<S>> page(long top, long skip) throws IOException {
        ResponseEntity<String> response = doGet("/?top=" + top +"&skip=" + skip,String.class);
        if(response.getStatusCode() == HttpStatus.NOT_FOUND){
            return Optional.empty();
        } else if(response.getStatusCode().is2xxSuccessful()){
            try {
                JsonNode node = mapper.readTree(response.getBody());
                WithoutContentPagedResult result = mapper.convertValue(node, WithoutContentPagedResult.class);
                JsonNode array = node.get("content");
                if (array.isArray()) {
                    List<S> content = parseFromJsonList(array);
                    return Optional.of(result.toPagedResult(content));
                }
                return Optional.of(result.toPagedResult(Collections.emptyList()));
            }catch(Throwable t) {
                throw new IOException("Error parsing response: " + ((HttpStatus) response.getStatusCode()).getReasonPhrase());
            }
        } else {
            throw new IOException("Unexpected server response:" + response.getStatusCode());
        }
    }

     public <S extends T> S create(S dto) throws IOException {
         ResponseEntity<String> response =  restTemplate.postForEntity(prefix,dto,String.class);
         if(!response.getStatusCode().is2xxSuccessful()) {
             throw new IOException("error creating new entity: " + ((HttpStatus) response.getStatusCode()).getReasonPhrase());
         }
         JsonNode node = mapper.readTree(response.getBody());
         return parseFromJson(node);
     }

     protected <R> ResponseEntity<R> doGet(String pathAfterPrefix, Class<R> responseClass){
        return doGet(pathAfterPrefix, null, responseClass);

     }

    /**
     * perform a HTTP GET request and get back the response with the given datatype.
     * @param pathAfterPrefix the path of the GSRS REST API after the prefix.  For example,
     *                        if the prefix is <pre>api/v1/substances</pre> and you want to perform a count query,
     *                        then this parameter should be <pre>/@count</pre> note the leading slash.
     *                        If you want to do an ID lookup the this parameter should be
     *                        <pre>(12345)</pre> note no leading slash since we want the requested url to be
     *                        <pre>pi/v1/substances(12345)</pre>.
     * @param view optional view(s) to use; if no view is required this should be set to {@code null}.
     * @param responseClass the class type of the response; often String.
     * @param <R> the class object for responseClass
     * @return a ResponseEntity with the Result of the REST API call.
     */
    protected <R> ResponseEntity<R> doGet(String pathAfterPrefix, String view, Class<R> responseClass){
        String url = pathAfterPrefix==null? prefix: prefix+pathAfterPrefix;

        if(view !=null && !view.trim().isEmpty()){
            boolean set=false;
            if(pathAfterPrefix !=null){
                if(pathAfterPrefix.contains("?")){
                    url += "&view="+view;
                    set=true;
                }
            }
            if(!set){
                url +="?view=" +view;
            }
        }

        return restTemplate.getForEntity(url, responseClass);

    }

    public <S extends T> S update(S dto) throws IOException{
        I id = getIdFrom(dto);
        //rest template PUT is void need to use lower level exchange to get response obj...
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<T> entity = new HttpEntity<>(dto, headers);

        ResponseEntity<String> response =  restTemplate.exchange(prefix+"("+id+")", HttpMethod.PUT, entity,String.class);
        if(!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("error creating new entity: " + ((HttpStatus) response.getStatusCode()).getReasonPhrase());
        }
        JsonNode node = mapper.readTree(response.getBody());
        return parseFromJson(node);
    }

    protected abstract <S extends T> S parseFromJson(JsonNode node);
    protected <S extends T> List<S> parseFromJsonList(JsonNode node){
        List<S> list = new ArrayList<>(node.size());
        for(JsonNode n : node){
            list.add(parseFromJson(n));
        }
        return list;
    }
    @Data
    @Builder
    public static class PagedResult<T>{

        private long id;
        private int version;
        private Date created;
        @JsonProperty("etag")
        private String etagId;
        private String path;
        private URI uri;
        private URI nextPageUri;

        private long total;
        private long top;
        private long skip;
        private long count;

        private List<? super T> content;

    }

    /**
     * By default, the RestTemplate will throw exceptions
     * on 4xx and 5xx errors before we can process them,
     * so we make a custom handler that does nothing
     * so we can read the JSON response and handle it in our code.
     *
     */
    enum RestTemplateResponseErrorHandler
            implements ResponseErrorHandler {
        INSTANCE;

        @Override
        public boolean hasError(ClientHttpResponse httpResponse)
                throws IOException {

            HttpStatus.Series s = ((HttpStatus) httpResponse.getStatusCode()).series();
            return (s == HttpStatus.Series.CLIENT_ERROR || s == HttpStatus.Series.SERVER_ERROR);
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse)
                throws IOException {

            //do nothing we will handle it downstream
        }
    }

    @Data
    public static class ExistsCheckResult{
        private Map<String, EntityExists> found;
        private List<String> notFound;
    }
    @Data
    public static class EntityExists{
        private String id;
        private String query;
        private String url;

    }
}
