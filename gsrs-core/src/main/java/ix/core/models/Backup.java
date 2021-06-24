package ix.core.models;

import java.lang.annotation.*;

/**
 * Annotation to put on an entity that implements
 * the {@link FetchableEntity} interface that tells
 * GSRS systems that this entity should be backed up.
 */
@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Inherited
@Target(value={ElementType.TYPE})
public @interface Backup {
    
}
