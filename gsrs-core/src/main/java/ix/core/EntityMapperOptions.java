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
}
