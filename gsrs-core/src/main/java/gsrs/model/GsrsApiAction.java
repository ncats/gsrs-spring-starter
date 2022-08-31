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

    /**
     * Should the JSON serialized object be just the url as a String or
     * if the object should be an Object that contains
     * both the url field and also
     *  the HTTP verb type.
     * @return {@code true} if only the url should be serialized;
     * {@code false} if the url and verb should be
     */
    boolean serializeUrlOnly() default false;
    /**
     * The HTTP verb Type; defaults to GET
     * @return
     */
    Type type() default Type.GET;

    boolean isRaw() default false;

    enum Type{
        GET,
        PUT,
        POST,
        DELETE,
        HEAD,
        PATCH
    }

    /**
     * What view classes this Action should be visible for. This should be the same
     * classes that would normally be put in {@link com.fasterxml.jackson.annotation.JsonView}.
     * @return an array of Classes, if not set then empty array.
     */
    Class<?>[] view() default {};
}
