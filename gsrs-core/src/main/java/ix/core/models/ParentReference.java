package ix.core.models;

import java.lang.annotation.*;

/**
 * A Marker Annotation to denote that a field
 * is referencing a Parent object.
 * This should be used to annotate fields that point to parents
 * in Entity objects that also are passed around as JSON
 * so that we can correctly reconstruct to object tree
 * if the JSON lacks the parent reference.
 */
@Documented
@Retention(value= RetentionPolicy.RUNTIME)
@Inherited
@Target(value={ElementType.FIELD})
public @interface ParentReference {
}
