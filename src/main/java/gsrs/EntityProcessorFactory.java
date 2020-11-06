package gsrs;

import ix.core.CombinedEntityProcessor;
import ix.core.EntityProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EntityProcessorFactory {
    private static Object MAP_VALUE_TOKEN = new Object();
    @Autowired(required = false)
    private List<EntityProcessor> entityProcessors;

    private Map<Class, List<EntityProcessor>> processorMapByClass = new ConcurrentHashMap<>();
    private Map<Class, EntityProcessor> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init(){
        //entityProcessors field may be null if there's no EntityProcessor to inject
        if(entityProcessors !=null) {
            for (EntityProcessor ep : entityProcessors) {
                Class entityClass = ep.getEntityClass();
                if (entityClass == null) {
                    continue;
                }
                processorMapByClass.computeIfAbsent(entityClass, k -> new ArrayList<>()).add(ep);
            }
        }
    }

    public EntityProcessor getCombinedEntityProcessorFor(Object o){
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
