package gsrs;

import gov.nih.ncats.common.util.CachedSupplier;
import ix.core.CombinedEntityProcessor;
import ix.core.EntityProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


public abstract class AbstractEntityProcessorFactory implements EntityProcessorFactory {
    private static Object MAP_VALUE_TOKEN = new Object();


    private Map<Class, List<EntityProcessor>> processorMapByClass = new ConcurrentHashMap<>();
    private Map<Class, EntityProcessor> cache = new ConcurrentHashMap<>();

    private final CachedSupplier<Void> initializer = ENTITY_PROCESSOR_FACTORY_INITIALIZER_GROUP.add(CachedSupplier.ofInitializer(()->{
        //entityProcessors field may be null if there's no EntityProcessor to inject
        processorMapByClass.clear();
        cache.clear();
        registerEntityProcessor(ep -> {
            Class entityClass = ep.getEntityClass();
            if (entityClass != null) {

                processorMapByClass.computeIfAbsent(entityClass, k -> new ArrayList<>()).add(ep);
            }
        });
    }));
    @PostConstruct
    public void init(){
        initializer.getSync();

    }

    /**
     * Reset the Cache of known EntityProcessors.  This method should
     * be called whenever a new EntityProcessor is added after initialization.
     */
    protected final void resetCache(){
        initializer.resetCache();

    }
    protected abstract void registerEntityProcessor(Consumer<EntityProcessor> registar);



    @Override
    public EntityProcessor getCombinedEntityProcessorFor(Object o){
        initializer.getSync();
        Class entityClass = o.getClass();
        return cache.computeIfAbsent(entityClass, k-> {
            Map<EntityProcessor, Object> list = new IdentityHashMap<>();

            for (Map.Entry<Class, List<EntityProcessor>> entry : processorMapByClass.entrySet()) {
                if (entry.getKey().isAssignableFrom(k)) {
                    for (EntityProcessor ep : entry.getValue()) {
                        list.put(ep, MAP_VALUE_TOKEN);
                    }
                }
            }
            Set<EntityProcessor> processors = list.keySet();
            //TODO add legacy re-indexer?
            //TODO add backup processor?
            if(processors.isEmpty()){
                return new NoOpEntityProcessor(k);
            }
            return new CombinedEntityProcessor(k, processors);
        });
    }

    /**
     * an EntityProcessor that does nothing.  This is used when we don't have any registered entity processors to combine.
     * @param <T>
     */
    private static class NoOpEntityProcessor<T> implements EntityProcessor<T>{
        private final Class<T> c;

        public NoOpEntityProcessor(Class<T> c) {
            this.c = c;
        }

        @Override
        public Class<T> getEntityClass() {
            return c;
        }
    }
}
