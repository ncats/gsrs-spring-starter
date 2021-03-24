package gsrs.controller.hateoas;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import gsrs.controller.AbstractGsrsEntityController;
import gsrs.controller.GsrsEntityController;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;

import java.util.*;

/**
 * GSRS Controller Aware HATEOAS {@link RepresentationModel} implementation
 * so we can add GSRS specific links in a standard way.
 * This is unwrapped so that we can add the links directly to the obj's JSON Node
 * instead of to a "links" object
 * @param <T>
 *
 * @see {@link GsrsUnwrappedEntityModelProcessor}
 */
public class GsrsUnwrappedEntityModel<T> extends RepresentationModel<GsrsUnwrappedEntityModel<T>> {

    @JsonIgnore
    private boolean isCompact;

    private T obj;
    /**
     * This is the list of Links we will in-line these as well
     * using JsonAnyGetter on the getter below.
     */
    private Map<String, Object> ourLinks = new HashMap<>();

    public static <T> GsrsUnwrappedEntityModel<T> of(T obj){
        if(obj instanceof Collection){
            return new CollectionGsrsUnwrappedEntityModel<>(obj);
        }
        return new GsrsUnwrappedEntityModel<T>(obj);
    }
    protected GsrsUnwrappedEntityModel(T obj) {
        this.obj = obj;
    }


    @Override
    public GsrsUnwrappedEntityModel<T> add(Link link) {
         ourLinks.put(link.getRel().value(), new RestUrlLink(link.getHref()));
         return this;
    }
    public GsrsUnwrappedEntityModel<T> add(Link link, String type) {
        ourLinks.put(link.getRel().value(), new RestUrlLink(link.getHref(), type));
        return this;
    }
    public GsrsUnwrappedEntityModel<T> addLink(String name, String href){
        ourLinks.put(name, new RestUrlLink(href));
        return this;
    }
    @JsonAnyGetter
    public Map<String, Object> getOurLinks() {
        return ourLinks;
    }

    @JsonIgnore
    public boolean isCompact() {
        return isCompact;
    }

    public void setCompact(boolean compact) {
        isCompact = compact;
    }
    @JsonUnwrapped
    public T getObj() {
        return obj;
    }

    @Data
    @AllArgsConstructor
    public static class RestUrlLink{
        private String url;
        private String type;

        public RestUrlLink(String url){
            this(url, "GET");
        }
    }
}
