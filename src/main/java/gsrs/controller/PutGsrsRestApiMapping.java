package gsrs.controller;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An HTTP Put Mapping for a {@link GsrsRestApiController} annotated controller.
 *
 * Leading Slashes in the paths matter.  Unlike normal Spring
 * DeleteMapping which always adds a leading slash these routes respect if you put a leading slash
 * or not.  This allows you to write API routes like {@code api/context(id)} by making the path= "(id)".
 *
 * Use the ID Placeholder strings where you want to put your ID regular expressions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@RequestMapping(
        method = {RequestMethod.PUT}
)
@GsrsRestApiRequestMapping
public @interface PutGsrsRestApiMapping {
    /**
     * Alias for {@link GsrsRestApiRequestMapping#apiVersions()}.
     */
    int[] apiVersions() default {1, 2};
    /**
     * Alias for {@link GsrsRestApiRequestMapping#value()}.
     */
    @AliasFor(annotation = GsrsRestApiRequestMapping.class,  value= "value")
    String[] value() default {};
    /**
     * Alias for {@link GsrsRestApiRequestMapping#idPlaceholder()}.
     */
    String idPlaceholder() default "$ID";
    /**
     * Alias for {@link GsrsRestApiRequestMapping#notIdPlaceholder()}.
     */
    String notIdPlaceholder() default "$NOT_ID";

    /**
     * Alias for {@link RequestMapping#name}.
     */
    @AliasFor(annotation = GsrsRestApiRequestMapping.class)
    String name() default "";


    /**
     * Alias for {@link GsrsRestApiRequestMapping#path()}.
     */
    @AliasFor(annotation = GsrsRestApiRequestMapping.class)
    String[] path() default {};

    /**
     * Alias for {@link RequestMapping#params}.
     */
    @AliasFor(annotation = GsrsRestApiRequestMapping.class)
    String[] params() default {};

    /**
     * Alias for {@link RequestMapping#headers}.
     */
    @AliasFor(annotation = GsrsRestApiRequestMapping.class)
    String[] headers() default {};

    /**
     * Alias for {@link RequestMapping#consumes}.
     * @since 4.3.5
     */
    @AliasFor(annotation = GsrsRestApiRequestMapping.class)
    String[] consumes() default {};

    /**
     * Alias for {@link RequestMapping#produces}.
     */
    @AliasFor(annotation = GsrsRestApiRequestMapping.class)
    String[] produces() default {};

}
