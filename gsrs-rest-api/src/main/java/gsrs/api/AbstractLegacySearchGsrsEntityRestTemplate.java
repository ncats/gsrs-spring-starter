package gsrs.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.util.SanitizerUtil;
import lombok.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.*;

public abstract class AbstractLegacySearchGsrsEntityRestTemplate<T,I> extends GsrsEntityRestTemplate<T, I>{

    public AbstractLegacySearchGsrsEntityRestTemplate(RestTemplateBuilder restTemplateBuilder, String baseUrl, String context, ObjectMapper mapper) {
        super(restTemplateBuilder, baseUrl, context, mapper);
    }

    public AbstractLegacySearchGsrsEntityRestTemplate(RestTemplateBuilder restTemplateBuilder, String baseUrl, String context) {
        super(restTemplateBuilder, baseUrl, context);
    }

    protected Class<? extends SearchResult> getSearchResultClass(){
        return SearchResult.class;
    }

    public SearchResult<T> search(SanitizedSearchRequest searchRequest) throws IOException {
        boolean hasParams=false;
        StringBuilder builder = new StringBuilder("/search");
        if(searchRequest.q !=null && !searchRequest.q.isEmpty()){
            hasParams = true;
            builder.append("?q="+searchRequest.q);
        }

        Iterator<Map.Entry<String,Object>> iter = searchRequest.getParameterMap().entrySet().iterator();
        if(iter.hasNext()){
            if(!hasParams){
                builder.append("?");
            }else{
                builder.append("&");
            }
            Map.Entry<String,Object> entry = iter.next();
            builder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        while(iter.hasNext()){
            Map.Entry<String,Object> entry = iter.next();
            builder.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        }
        ResponseEntity<String> response = doGet(builder.toString(), String.class);
        //stupid hack remove content parse it and add it back

        JsonNode node = getObjectMapper().readTree(response.getBody());
        JsonNode array = ((ObjectNode)node).remove("content");


        if(response.getStatusCode().is2xxSuccessful()) {

            SearchResult<T> result =  getObjectMapper().convertValue(node, getSearchResultClass());
            if(array.isArray()) {
                List<T> content = parseFromJsonList(array);
                result.setContent(content);
            }
            return result;
        }
        throw new IOException(node.get("/message").asText());
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class SearchResult<T>{
        private String etag;
        private String uri;
        private String nextPageUri;

        private int total, count, skip, top;

        private String query;

        private List<Facet> facets;

        private List<T> content = new ArrayList<>();

        public Optional<Facet> getFacet(String name){
            Objects.requireNonNull(name);
            return facets.stream().filter(f-> name.equals(f.getName())).findFirst();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Facet{
        private String name;
        private boolean enchanced;
        private String prefix;
        private List<FacetValue> values = new ArrayList<>();

        public Optional<FacetValue> getFacetValue(String label){
            Objects.requireNonNull(label);
            return values.stream().filter(facetValue -> label.equals(facetValue.getLabel())).findFirst();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FacetValue{
        private String label;
        private int count;
    }
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchRequest{

        private String q;

        private Integer top;

        private Integer skip;

        private Integer fdim;

        private String order;

        private String field;

        public SanitizedSearchRequest sanitize(){
            return new SanitizedSearchRequest(this);
        }
    }


    @Getter
    @EqualsAndHashCode
    public static class SanitizedSearchRequest{
        private static final int DEFAULT_TOP =10;
        private static final int DEFAULT_FDIM =10;

        private static final String DEFAULT_FIELD= "";
        private String q;

        private int top;

        private int skip;

        private int fdim;
        private String field;
        private String order;

        private SanitizedSearchRequest(SearchRequest request){
            this.top = SanitizerUtil.sanitizeInteger(request.top, DEFAULT_TOP);
            this.skip = SanitizerUtil.sanitizeInteger(request.skip, 0);
            this.fdim = SanitizerUtil.sanitizeInteger(request.fdim, DEFAULT_FDIM);

            this.q = request.q ==null? null: request.q;//don't trim it breaks mol format!
            this.order = request.order;
            this.field = request.field ==null? DEFAULT_FIELD: request.field;
        }

        public static String getDefaultField() {
            return DEFAULT_FIELD;
        }

        public static int getDefaultTop() {
            return DEFAULT_TOP;
        }

        public static int getDefaultFdim() {
            return DEFAULT_FDIM;
        }


        public Map<String,Object> getParameterMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("top", top);
            map.put("skip", skip);
            map.put("fdim", fdim);
            if(order !=null) {
                map.put("order", order);
            }
            if(!field.trim().isEmpty()){
                map.put("field", field.trim());
            }
            return map;
        }
    }
}
