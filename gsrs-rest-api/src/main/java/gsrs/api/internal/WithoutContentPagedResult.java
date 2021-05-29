package gsrs.api.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gsrs.api.GsrsEntityRestTemplate;
import gsrs.controller.AbstractGsrsEntityController;
import lombok.Data;

import java.net.URI;
import java.util.Date;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WithoutContentPagedResult {
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

    public <T> GsrsEntityRestTemplate.PagedResult<T> toPagedResult(List<T> content){
        return GsrsEntityRestTemplate.PagedResult.<T>builder()
                .content(content)
                .id(id)
                .version(version)
                .created(created)
                .etagId(etagId)
                .path(path)
                .uri(uri)
                .nextPageUri(nextPageUri)
                .total(total)
                .top(top)
                .skip(skip)
                .count(count)
                .build();
    }

}
