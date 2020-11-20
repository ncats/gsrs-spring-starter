package gsrs.indexer;

import gov.nih.ncats.common.util.CachedSupplier;
import ix.core.search.text.CombinedIndexValueMaker;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.ReflectingIndexValueMaker;
import ix.core.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
/**
 * An abstract {@link IndexValueMakerFactory} implementation
 * that will create a combined IndexValueMaker containing the ReflectingIndexValueMaker
 * plus any compatibile IndexValueMakers that are registered with {@link #registerIndexValueMakers(Consumer)}
 * method.
 *
 */
public abstract class AbstractIndexValueMakerFactory implements IndexValueMakerFactory {
    private ReflectingIndexValueMaker reflectingIndexValueMaker = new ReflectingIndexValueMaker();

    private ConcurrentHashMap<Class, List<IndexValueMaker>> valueMakersMap = new ConcurrentHashMap<>();
    private final CachedSupplier<Void> initializer = CachedSupplier.runOnce(()->{
        registerIndexValueMakers(i->{
            if(i !=null) {
                valueMakersMap.computeIfAbsent(i.getIndexedEntityClass(), c -> new ArrayList<>()).add(i);
            }
        });
       return null;
    });

    /**
     * Initization method that is called to register IndexValueMakers
     * to this factory.  This method is only called once the first
     * time any createIndexValueMakerFor() method is called.
     * @param registrar a Consumer of IndexValueMaker instances; will never be null.
     */
    protected abstract void registerIndexValueMakers(Consumer<IndexValueMaker> registrar);


    /**
     * Create a IndexValueMaker that contains all the registered
     * IndexValueMakers that apply to the given passed in {@link EntityUtils.EntityWrapper}.
     * There is currently one default indexValueMaker, the {@link ReflectingIndexValueMaker}
     * that is included on all objects.
     * If more indexvalue makers are found to apply to a given entity,
     * then they are merged into a composite {@link IndexValueMaker} instance
     * to hide it from the caller.
     *
     * @param ew the {@link EntityUtils.EntityWrapper} to be indexed;
     *           can not be {@code null}.
     *
     * @return a {@link IndexValueMaker} which may have been cached from previous calls
     * so no guarantees are given that each call is a new instance.
     */
    @Override
    public  IndexValueMaker createIndexValueMakerFor(EntityUtils.EntityWrapper<?> ew){
        initializer.getSync();
        Class<?> clazz = ew.getEntityClass();
        List<IndexValueMaker> acceptedList = new ArrayList<>();
        //always add reflecting indexvaluemaker
        acceptedList.add(reflectingIndexValueMaker);

            for (Map.Entry<Class, List<IndexValueMaker>> entry : valueMakersMap.entrySet()) {
                if (entry.getKey().isAssignableFrom(clazz)) {
                    acceptedList.addAll(entry.getValue());
                }
            }

        return new CombinedIndexValueMaker(clazz, acceptedList);

    }
}
