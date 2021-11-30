package ix.core.models;

import java.lang.annotation.*;

/**
 * A Marker Annotation to denote that a field
 * is referencing a Parent object.
 * This should be used to annotate fields that point to their one parent
 * in Entity objects that also are passed around as JSON
 * so that we can correctly reconstruct to object tree
 * if the JSON lacks the parent reference.
 *
 * If annotated on a field, then the field object type should be appropatie for
 * all possible values the JSON structure can have as a parent.
 *
 * If annotated on a method, then the method signature should return void and have one parameter
 * that is a type that is appropirate for all possible values the JSON structure can have as a parent.
 *
 * If annotated on a field, then that is the parent field that is the
 * parent/owner of this entity.  If annotated on a method, then that method
 * can decide if any fields should be set based on the passed in owner object.
 */
@Documented
@Retention(value= RetentionPolicy.RUNTIME)
@Inherited
@Target(value={ElementType.FIELD, ElementType.METHOD})
public @interface ParentReference {
}
