package gsrs.indexer;

import ix.core.search.text.CombinedIndexValueMaker;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.ReflectingIndexValueMaker;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class that finds all the {@link IndexValueMaker}
 * components in the component scan and maps figures out which
 * ones should be called for each entity.
 *
 * There is currently one default indexValueMaker, the {@link ReflectingIndexValueMaker}
 * that is included on all objects.
 *
 * If more than one indexvalue maker is found to apply to a given entity,
 * then they are merged into a composite {@link IndexValueMaker} instance
 * to hide it from the caller.
 */
@Service
public class IndexValueMakerFactory {


    @Autowired(required = false)
    private List<IndexValueMaker> indexValueMakers;
    private ReflectingIndexValueMaker reflectingIndexValueMaker = new ReflectingIndexValueMaker();

    /**
     * Create a IndexValueMaker that contains all the registered
     * IndexValueMakers that apply to the given passed in entity object.
     * There is currently one default indexValueMaker, the {@link ReflectingIndexValueMaker}
     * that is included on all objects.
     * If more indexvalue makers are found to apply to a given entity,
     * then they are merged into a composite {@link IndexValueMaker} instance
     * to hide it from the caller.
     *
     * @param obj the entity object to be indexed; can not be {@code null}.
     *
     * @return a {@link IndexValueMaker} which may have been cached from previous calls
     * so no guarantees are given that each call is a new instance.
     */
    public IndexValueMaker createIndexValueMakerFor(Object obj){
        return createIndexValueMakerFor( EntityUtils.EntityWrapper.of(obj));
    }
    /**
     * Create a IndexValueMaker that contains all the registered
     * IndexValueMakers that apply to the given passed in {@link ix.core.util.EntityUtils.EntityWrapper}.
     * There is currently one default indexValueMaker, the {@link ReflectingIndexValueMaker}
     * that is included on all objects.
     * If more indexvalue makers are found to apply to a given entity,
     * then they are merged into a composite {@link IndexValueMaker} instance
     * to hide it from the caller.
     *
     * @param ew the {@link ix.core.util.EntityUtils.EntityWrapper} to be indexed;
     *           can not be {@code null}.
     *
     * @return a {@link IndexValueMaker} which may have been cached from previous calls
     * so no guarantees are given that each call is a new instance.
     */
    public  IndexValueMaker createIndexValueMakerFor(EntityUtils.EntityWrapper<?> ew){
        //indexValueMakers field is null if no components found
        if(indexValueMakers ==null) {
            return reflectingIndexValueMaker;
        }
        Class<?> clazz = ew.getEntityClass();
        List<IndexValueMaker> acceptedList = new ArrayList<>();
        //always add reflecting indexvaluemaker
        acceptedList.add(reflectingIndexValueMaker);

            for (IndexValueMaker indexValueMaker : indexValueMakers) {
                Class<?> c = indexValueMaker.getIndexedEntityClass();
                if (c.isAssignableFrom(clazz)) {
                    acceptedList.add(indexValueMaker);
                }
            }

        return new CombinedIndexValueMaker(clazz, acceptedList);

    }
}
