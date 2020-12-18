package gsrs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * A special annotation that denotes that the annotated
 * method should be an action that can be invoked
 * by the REST API.
 *
 * The method should return a {@link ix.core.ResourceReference}
 * or one of its subclasses.
 */
@Documented
@Retention(value= RetentionPolicy.RUNTIME)
@Target(value= ElementType.METHOD)
public @interface GsrsApiAction {
    /**
     * The name this action should have in the API response.
     * @return
     */
    String value();
}
