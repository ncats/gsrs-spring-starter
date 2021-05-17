package ix.core.models;

import java.lang.annotation.*;

/**
 * Denotes this class should begin the root of an index path.
 * In practice this means the entity has its own
 * Controller.
 *
 * @since 3.0
 */
@Documented
@Retention(value= RetentionPolicy.RUNTIME)
@Target(value={ElementType.TYPE})
public @interface IndexableRoot {
}
