package gsrs.controller;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rest Controller marker for a GSRS Entity API.
 *
 * This Annotation is used to note that a given
 * controller is a GSRS controller so all the GSRS
 * standard API route paths are generated.
 *
 * @see GetGsrsRestApiMapping
 * @see PostGsrsRestApiMapping
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@RestController
@RequestMapping
public @interface GsrsRestApiController {
    /**
     * This sets the route entity "context" in GSRS parlance.
     * The context is the root entity name in the route in the format
     * `api/v1/$context`.  For example, if this entity we are making our controller
     * for is for Substances, then we set the context to "substances"
     * and the GSRS API will become `api/v1/substances`.
     *
     * @apiNote : to conform to Spring RequestMapping, the context
     * can technically be an array of values but it should only ever be set to a single value.
     * @return the single String value for this entity context, for example "substances".
     */
    @AliasFor(annotation = RequestMapping.class, attribute = "value")
    String[] context();

    /**
     * Set the {@link IdHelper} used for the Entity's ID.  This is
     * used to generate the correct regular expressions in the standard route mappings.
     * If not explicitly set, the default is UUID.
     * @return
     */
    IdHelpers idHelper() default IdHelpers.UUID;

    Class<? extends IdHelper> customIdHelperClass() default IdHelper.class;

    String customIdHelperClassName() default "";

}

