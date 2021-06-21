package gsrs.controller.hateoas;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import ix.core.controllers.EntityFactory;
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
    @JsonIgnore
    private T obj;

    /**
     * This is the list of Links we will in-line these as well
     * using JsonAnyGetter on the getter below.
     */
    private Map<String, Object> ourLinks = new LinkedHashMap<>();

    public static <T> GsrsUnwrappedEntityModel<T> of(T obj, String view){
        if(obj instanceof Collection){
            return new CollectionGsrsUnwrappedEntityModel<>(obj, view);
        }
        return new GsrsUnwrappedEntityModel<T>(obj, view, true);
    }
    protected GsrsUnwrappedEntityModel(T obj, String view, boolean includeUnserialized) {

        this.obj = obj;
        if(includeUnserialized) {
            //katzelda June 2021: This is a terrible hack because HATEOS has its own jackson converter so we can't
            //use our view=$X parameter like we can in our Spring bean converter because the HATEOS doesn't use it
            //so the work around is to serialize it ourselves and then add it to the unwrapped properties map here
            //so it looks like we serialized the object as expected with additional url properties...
            EntityFactory.EntityMapper entityMapper = EntityFactory.EntityMapper.getByView(view);
            this.ourLinks.putAll(entityMapper.convertValue(entityMapper.toJsonNode(obj), new TypeReference<Map<String, Object>>() {
            }));
        }
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
//    @JsonUnwrapped
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