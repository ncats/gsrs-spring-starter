package gsrs.indexer;

import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.ReflectingIndexValueMaker;
import ix.core.util.EntityUtils;

public interface IndexValueMakerFactory {
    /**
     * Create a IndexValueMaker that contains all the registered
     * IndexValueMakers that apply to the given passed in entity object.
     *
     *
     * @param obj the entity object to be indexed; can not be {@code null}.
     *
     * @return a {@link IndexValueMaker} which may have been cached from previous calls
     * so no guarantees are given that each call is a new instance.
     */
    default IndexValueMaker createIndexValueMakerFor(Object obj){
        return createIndexValueMakerFor( EntityUtils.EntityWrapper.of(obj));

    }
    /**
     * Create a IndexValueMaker that contains all the registered
     * IndexValueMakers that apply to the given passed in {@link ix.core.util.EntityUtils.EntityWrapper}.
     *
     *
     * @param ew the {@link ix.core.util.EntityUtils.EntityWrapper} to be indexed;
     *           can not be {@code null}.
     *
     * @return a {@link IndexValueMaker} which may have been cached from previous calls
     * so no guarantees are given that each call is a new instance.
     */
    IndexValueMaker createIndexValueMakerFor(EntityUtils.EntityWrapper<?> ew);
}
