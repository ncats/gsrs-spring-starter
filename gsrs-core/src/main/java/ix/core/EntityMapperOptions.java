package ix.core;

import java.lang.annotation.*;

/**
 * Extra Options and Hints used by EntityMapper.
 */
@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Inherited
@Target(value={ElementType.TYPE})
public @interface EntityMapperOptions {
    /**
     * When Json View {@link ix.core.models.BeanViews.Key} is used
     * should this entity be serialized as just the id and kind fields?
     * @return {@code true} if should be collapsed; {@code false} otherwise.
     * defaults to {@code true}.
     */
    boolean collapsibleInKeyView() default true;
}
