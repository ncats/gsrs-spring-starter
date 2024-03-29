package ix.core;

import java.lang.annotation.*;

/**
 * Extra Options and Hints used by EntityMapper.
 */
@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Inherited
@Target(value={ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface EntityMapperOptions {
    /**
     * When Json View {@link ix.core.models.BeanViews.Key} is used
     * should this entity be serialized as just the id and kind fields?
     * @return {@code true} if should be collapsed; {@code false} otherwise.
     * defaults to {@code true}.
     */
    boolean collapsibleInKeyView() default true;

    boolean linkoutInCompactView() default false;
    String linkoutInCompactViewName() default "";

    boolean includeAsCallable() default false;

    boolean linkoutRawInEveryView() default false;

    /**
     * The href for the self URL; defaults to "_self".
     * @return
     */
    String getSelfRel() default "_self";

    /**
     * The JsonViews that need to be present
     * to include the self rel.
     * @return
     */
    Class<?>[] selfRelViews() default {};
    /**
     * Name of method of class that provides
     * the id as a String.  If not set, (or set to {@code ""} then
     * use the field or method annotated with @Id.
     * @return the String of the public method name that takes no arguments and returns a String
     * that is the id.
     */
    String idProviderRef() default "";

}
