package gsrs.model;

import java.lang.annotation.*;

/**
 * Annotation on getter methods
 * to ignore this method for the purpose of
 * assertion matching in tests.
 */
@Documented
@Retention(value= RetentionPolicy.RUNTIME)
@Target(value=ElementType.METHOD)
public @interface MatchingIgnore {
}
