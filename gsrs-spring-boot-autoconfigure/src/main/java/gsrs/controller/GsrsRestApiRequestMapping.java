package gsrs.controller;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parent meta-annotation for all GSRS custom request mappings
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD}) //need TYPE annotation to make it meta-annotation
@RequestMapping
public @interface GsrsRestApiRequestMapping {
    /**
     * which API versions should be generated for this request mapping,
     * if set to {@code {1, 2}} then this will make 2 routes: {@code api/v1/<your route>} and {@code api/v2/<your route>}.
     * @return
     */
    int[] apiVersions() default {1, 2};

    /**
     *  Path for this route but unlike normal Spring routes, leading slashes in the paths matter!
     *  Unlike normal Spring RequestMapping which always adds a leading slash;
     *  these routes respect if you put a leading slash or not.
     *  This allows you to write API routes like {@code api/v1/context(id)} by making the path= "(id)".
     *  This value also supports adding the place holders for {@link #idPlaceholder()}
     *  and {@link #notIdPlaceholder()}.
     *
     * @return the path as a String.
     */
    @AliasFor(annotation = RequestMapping.class,  value= "value")
    String[] value() default {};
    /**
     * The placeholder String to represent an ID in the route;
     * by default it is "$ID". Whenever the route path contains
     * this placeholder, the placeholder will be replaced by the regular expression
     * for that {@link IdHelper} implementation defined in the {@link AbstractGsrsEntityController}.
     * @return A String can not be null or empty.
     */
    String idPlaceholder() default "$ID";
    /**
     * The placeholder String to represent "not an ID" in the route;
     * by default it is "$NOT_ID". Whenever the route path contains
     * this placeholder, the placeholder will be replaced by the regular expression
     * for that {@link IdHelper} implementation defined in the {@link AbstractGsrsEntityController}.
     * @return A String can not be null or empty.
     */
    String notIdPlaceholder() default "$NOT_ID";

    /**
     * Alias for {@link RequestMapping#name}.
     */
    @AliasFor(annotation = RequestMapping.class)
    String name() default "";


    /**
     *  Path for this route but unlike normal Spring routes, leading slashes in the paths matter!
     *  Unlike normal Spring RequestMapping which always adds a leading slash;
     *  these routes respect if you put a leading slash or not.
     *  This allows you to write API routes like {@code api/v1/context(id)} by making the path= "(id)".
     *  This value also supports adding the place holders for {@link #idPlaceholder()}
     *  and {@link #notIdPlaceholder()}.
     *
     * @return the path as a String.
     */
    @AliasFor(annotation = RequestMapping.class)
    String[] path() default {};

    /**
     * Alias for {@link RequestMapping#params}.
     */
    @AliasFor(annotation = RequestMapping.class)
    String[] params() default {};

    /**
     * Alias for {@link RequestMapping#headers}.
     */
    @AliasFor(annotation = RequestMapping.class)
    String[] headers() default {};

    /**
     * Alias for {@link RequestMapping#consumes}.
     * @since 4.3.5
     */
    @AliasFor(annotation = RequestMapping.class)
    String[] consumes() default {};

    /**
     * Alias for {@link RequestMapping#produces}.
     */
    @AliasFor(annotation = RequestMapping.class)
    String[] produces() default {};

}
