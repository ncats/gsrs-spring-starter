package gsrs.controller.hateoas;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import ix.core.controllers.EntityFactory;
import ix.core.models.BeanViews;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.RepresentationModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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

    private static Pattern COMMA_SEP_PATTERN = Pattern.compile(",");
    private static Map<String, Class> viewMap = new CaseInsensitiveMap<>();

    private static Map<String, Set<Class>> singletonSet = new HashMap<>();
    static{
        for(Class c : BeanViews.class.getClasses()){
            String simpleName = c.getSimpleName();
            viewMap.put(simpleName, c);
            singletonSet.put(simpleName, Collections.singleton(c));

        }
        //handle no view case
        singletonSet.put(null, Collections.emptySet());
    }
    @JsonIgnore
    private boolean isCompact;
    @JsonIgnore
    private T obj;
    @JsonIgnore
    private Set<Class> views;

    private static Set<Class> parseViews(String views){
        if(views==null){
            return Collections.emptySet();
        }

        String[] split = COMMA_SEP_PATTERN.split(views);
        if(split.length==1){
            //special case 1
            return singletonSet.get(split[0]);

        }
        Set<Class> set = new HashSet<>();
        for(String v : split){
            Class c = viewMap.get(v);
            if(c !=null){
                set.add(c);
            }
        }
        return set;
    }

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
        //TODO: this needs to support cases where obj is primitive/String
        //it currently errors out in such a case.
        
        if(includeUnserialized) {
            //katzelda June 2021: This is a terrible hack because HATEOS has its own jackson converter so we can't
            //use our view=$X parameter like we can in our Spring bean converter because the HATEOS doesn't use it
            //so the work around is to serialize it ourselves and then add it to the unwrapped properties map here
            //so it looks like we serialized the object as expected with additional url properties...
            EntityFactory.EntityMapper entityMapper = EntityFactory.EntityMapper.getByView(view);
            Map<String, Object> m = entityMapper.convertValue(entityMapper.toJsonNode(obj), new TypeReference<Map<String, Object>>() {
            });
            //sometimes the map is null !?
            if(m!=null) {
                this.ourLinks.putAll(m);
            }
        }
        this.views = parseViews(view);
    }
    /**
     * Adds the link as a {@link GsrsCustomLink} which will handle its own json serialization.
     * @param customLink the Link object containing the url and the rel value which will be used as the name.
     * @return this
     */
    public GsrsUnwrappedEntityModel<T> add(GsrsCustomLink customLink) {
        Map<String, Object> properties = customLink.getCustomSerializedProperties();
        properties.put("url", customLink.getHref());

        ourLinks.put(customLink.getRel().value(), properties);
        return this;
    }
    /**
     * Adds the link as a {@link RestUrlLink} with a default type as GET.
     * @param link the Link object containing the url and the rel value which will be used as the name.
     * @return this
     */
    @Override
    public GsrsUnwrappedEntityModel<T> add(Link link) {
        if(link instanceof GsrsCustomLink){
            return add((GsrsCustomLink) link);
        }
         ourLinks.put(link.getRel().value(), new RestUrlLink(link.getHref()));
         return this;
    }
    /**
     * Adds the link as a as a plain String instead of as a {@link RestUrlLink}.
     * @param link the Link object containing the url and the rel value which will be used as the name.
     * @return this
     */
    public GsrsUnwrappedEntityModel<T> addRaw(Link link) {
        ourLinks.put(link.getRel().value(), link.getHref());
        return this;
    }
    /**
     * Adds the link as a as a plain String instead of as a {@link RestUrlLink}.
     * @param link the Link object containing the url and the rel value which will be used as the name.
     * @return this
     */
    public GsrsUnwrappedEntityModel<T> addRaw(GsrsCustomLink link) {
        ourLinks.put(link.getRel().value(), link.getHref());
        return this;
    }
    /**
     * Adds the link as a {@link RestUrlLink} with the given HTTP verb type.
     * @param link the Link object containing the url and the rel value which will be used as the name.
     * @param type the HTTP verb type (GET, POST etc) for this link.
     * @return this
     */
    public GsrsUnwrappedEntityModel<T> add(Link link, String type) {
        ourLinks.put(link.getRel().value(), new RestUrlLink(link.getHref(), type));
        return this;
    }

    /**
     * Adds the link as a {@link RestUrlLink} with a default type as GET.
     * @param name the name of the field being linked.
     * @param href the url as a String.
     * @return this
     */
    public GsrsUnwrappedEntityModel<T> addLink(String name, String href){
        ourLinks.put(name, new RestUrlLink(href));
        return this;
    }

    /**
     * Adds the and additional key-value pair object to the JSON.
     * @param key the name of the key.
     * @param value the value to add.
     * @return this
     */
    public GsrsUnwrappedEntityModel<T> addKeyValuePair(String key, Object value){
        ourLinks.put(key, value);
        return this;
    }

    /**
     * Adds the link as a plain String instead of as a {@link RestUrlLink}.
     * @param name the name of the field being linked.
     * @param href the url as a String.
     * @return this
     */
    public GsrsUnwrappedEntityModel<T> addRawLink(String name, String href){
        ourLinks.put(name, href);
        return this;
    }

    @Override
    @JsonIgnore
    public Links getLinks() {
        return super.getLinks();
    }

    @JsonAnyGetter
    public Map<String, Object> getOurLinks() {
        return ourLinks;
    }

    @JsonIgnore
    public boolean isCompact() {
        return isCompact;
    }

    public boolean hasView(Class v){
        if(views ==null || views.isEmpty()){
            return false;
        }
        if(views.contains(v)){
            return true;
        }
        //view Classes can extend others so it's possible that
        //we are asking if it has a view that is a PARENT class
        //of one of our views

        //internal extends JsonDiff
        //JsonDiff extends Full
        //we want hasView(JsonDiff) to be true if we have internal in our view list
        //but if we only have JsonDiff in our view we want hasView(Internal) to be false
        for(Class c : views){
            if(v.isAssignableFrom(c)){
                return true;
            }
        }
        return false;

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
