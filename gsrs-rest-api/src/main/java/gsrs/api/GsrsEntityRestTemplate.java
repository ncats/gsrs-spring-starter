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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public long count() throws IOException {
        ResponseEntity<String> response = restTemplate.getForEntity(prefix+"/@count",String.class);

        if(response.getStatusCode().is2xxSuccessful()) {
            return Long.parseLong(response.getBody());
        }
        throw new IOException("error getting count: " + response.getStatusCode().getReasonPhrase());
    }

    public Optional<T> findByResolvedId(String anyKindOfId) throws IOException{
        ResponseEntity<String> response = restTemplate.getForEntity(prefix+"("+anyKindOfId + ")",String.class);
        if(response.getStatusCodeValue() == 404) {
            return Optional.empty();
        }
        JsonNode node = mapper.readTree(response.getBody());
        return Optional.ofNullable(parseFromJson(node));

    }
    public Optional<T> findById(I id) throws IOException {
        return findByResolvedId(id.toString());
    }
    public boolean existsById(I id) throws IOException {
        ResponseEntity<String> response = restTemplate.getForEntity(prefix+"("+id + ")?view=key",String.class);
        if(response.getStatusCodeValue() == 404) {
            return false;
        }
        return true;
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

    public Optional<PagedResult<T>> page(long top, long skip) throws JsonProcessingException {
        ResponseEntity<String> response = restTemplate.getForEntity("/?top=" + top +"&skip=" + skip,String.class);
        if(response.getStatusCodeValue() == 404) {
            return Optional.empty();
        }
        JsonNode node = mapper.readTree(response.getBody());
        WithoutContentPagedResult result = mapper.convertValue(node, WithoutContentPagedResult.class);

        JsonNode array = node.get("content");
        if(array.isArray()){
            List<T> content = parseFromJsonList(array);
            return Optional.of(result.toPagedResult(content));
        }
        return Optional.of(result.toPagedResult(Collections.emptyList()));
    }

    protected abstract T parseFromJson(JsonNode node);
    protected List<T> parseFromJsonList(JsonNode node){
        List<T> list = new ArrayList<>(node.size());
        for(JsonNode n : node){
            list.add(parseFromJson(n));
        }
        return list;
    }
    @Data
    @Builder
    public static class PagedResult<T>{
        /*
        {"id":36301112,"version":1,"created":1622135343193,"etag":"f5ad600d20503a3b","path":"/app/api/v1/vocabularies/","uri":"https://ginas.ncats.nih.gov/app/api/v1/vocabularies/","nextPageUri":"https://ginas.ncats.nih.gov/app/api/v1/vocabularies/?skip=10","method":"GET","sha1":"3f7404a3eee82aec172d1590615eb2c7367399a1","total":78,"count":10,"skip":0,"top":10,"query":"","narrowSearchSuggestions":[],"content":
         */
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

        private List<T> content;

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

            return (
                    httpResponse.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR
                            || httpResponse.getStatusCode().series() == HttpStatus.Series.SERVER_ERROR);
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse)
                throws IOException {

            //do nothing we will handle it downstream
        }
    }

    @Data
    public static class ExistsCheckResult{
        private Map<String, GsrsEntityController.EntityExists> found;
        private List<String> notFound;
    }
    @Data
    public static class EntityExists{
        private String id;
        private String query;
        private RestUrl url;

    }
    @Data
    public static class RestUrl{
        private String rel;
        private URL href;
    }
}
