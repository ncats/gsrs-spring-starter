package ix.core.models;

import ix.core.util.EntityUtils;

public interface FetchableEntity {
    /**
     * An implementation
     * should ensure that the ID returned by this is
     * globally unique. For UUIDs, returning just the
     * UUID is sufficient. For sequence based
     * identifiers, adding some type information
     * (such as a prefix) is usually necessary.
     *
     * <p>
     * Note: this is less useful now that
     * {@link EntityUtils.EntityWrapper#getKey()} exists,
     * which returns a globally unique key that
     * can be used for fetching the models as well.
     * TODO: Refactor these to use the same mechanism by
     * default.
     * </p>
     * @return
     */
    String fetchGlobalId();
}
