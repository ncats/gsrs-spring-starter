package gsrs.controller.hateoas;

import org.springframework.hateoas.LinkRelation;

import java.util.Map;

/**
 * A customized URL link.
 */
public interface GsrsCustomLink {

    LinkRelation getRel();

    String getHref();

    /**
     * Return the mapping of customized fields as a new editable Map.
     * @return a new editable map which will never be null but may be empty.
     */
    Map<String, Object> getCustomSerializedProperties();
}
