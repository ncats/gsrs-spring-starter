package gsrs.model;

/**
 * An interface to denote that this object has a size
 * similar to Collection{@link #getSize()} but
 * this allows objects to not have to implement a Collection interface
 * and all the additional API requirements that come with that.
 * Used by {@link ix.core.EntityMapperOptions} during compact serialization
 * to include a size attribute if the object being serialized isn't a Collection or array.
 */
public interface Sizeable {

    int getSize();
}
