package gsrs.controller.hateoas;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import gsrs.controller.AbstractGsrsEntityController;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    /**
     * needed so the links to add are added to the object's JSON Node.
     */
    @JsonUnwrapped
    private T obj;
    /**
     * This is the list of Links we will in-line these as well
     * using JsonAnyGetter on the getter below.
     */
    private Map<String, Link> ourLinks = new HashMap<>();
    /**
     * This is the controller that was used to create this REST Response
     * will be used by the {@link GsrsUnwrappedEntityModelProcessor}
     * to generate the correct method URL.
     */
    @JsonIgnore
    private Class<? extends AbstractGsrsEntityController> controller;

    public GsrsUnwrappedEntityModel(T obj, Class<? extends AbstractGsrsEntityController> controllerClass) {
        this.obj = obj;
        this.controller = controllerClass;
    }

    public T getObj() {
        return obj;
    }

    public Class<? extends AbstractGsrsEntityController> getController() {
        return controller;
    }

    @Override
    public GsrsUnwrappedEntityModel<T> add(Link link) {
         ourLinks.put(link.getRel().value(), link);
         return this;
    }
    @JsonAnyGetter
    public Map<String, Link> getOurLinks() {
        return ourLinks;
    }
}
