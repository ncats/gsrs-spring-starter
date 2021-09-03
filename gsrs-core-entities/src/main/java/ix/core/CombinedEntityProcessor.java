package ix.core;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Wraps multiple {@link EntityProcessor}s behind a single implementation
 * so we can propagate to many processors but only need to hold a reference to one object.
 * IF any of the wrapped processors throw a Throwable, that throwable is caught, logged
 * and the CombinedEntityProcessor will continue to call the other processors.
 *
 * @implSpec The implementation of each method is basically:
 * <pre>
 * {@code
 * public void prePersist( Object o){
 *     for(EntityProcessor p : list){
 *         try{
 *             p.prePersist(o);
 *         }catch(Throwable t){
 *             log.warn(t);
 *         }
 *     }
 * }
 * }
 * </pre>
 * @param <K>
 */
@Slf4j
public class CombinedEntityProcessor<K> implements EntityProcessor<K> {
    private final List<EntityProcessor<? super K>> list;
    private final Class<K> clazz;

    public CombinedEntityProcessor(Class<K> clazz, EntityProcessor<? super K>... processors) {
        this.list = Arrays.asList(processors);
        this.clazz= clazz;
    }
    public CombinedEntityProcessor(Class<K> clazz,Collection<EntityProcessor<? super K>> processors) {
        this.list = new ArrayList<>(processors);
        this.clazz= clazz;
    }

    @Override
    public Class<K> getEntityClass() {
        return clazz;
    }

    @Override
    public void prePersist(K obj) throws FailProcessingException {
        for(EntityProcessor<? super K> processor : list){
            try{
                processor.prePersist(obj);
            }catch(Throwable e){
                log.warn(e.getMessage(),e);
            }
        }
    }

    @Override
    public void postPersist(K obj) throws FailProcessingException {
        for(EntityProcessor<? super K> processor : list){
            try{
                processor.postPersist(obj);
            }catch(Throwable e){
                log.warn(e.getMessage(),e);
            }
        }
    }

    @Override
    public void preRemove(K obj) throws FailProcessingException {
        for(EntityProcessor<? super K> processor : list){
            try{
                processor.preRemove(obj);
            }catch(Throwable e){
                log.warn(e.getMessage(),e);
            }
        }
    }

    @Override
    public void preUpdate(K obj) throws FailProcessingException {
        for(EntityProcessor<? super K> processor : list){
            try{
                processor.preUpdate(obj);
            }catch(Throwable e){
            log.warn(e.getMessage(),e);
            }
        }
    }

    @Override
    public void postRemove(K obj) throws FailProcessingException {
        for(EntityProcessor<? super K> processor : list){
            try{
                processor.postRemove(obj);
            }catch(Throwable e){
                log.warn(e.getMessage(),e);
            }
        }

    }


    @Override
    public void postUpdate(K obj) throws FailProcessingException {
        for(EntityProcessor<? super K> processor : list){
            try{
                processor.postUpdate(obj);
            }catch(Throwable e){
                log.warn(e.getMessage(),e);
            }
        }

    }

    @Override
    public void postLoad(K obj) throws FailProcessingException {
        for(EntityProcessor<? super K> processor : list){
            try{
                processor.postLoad(obj);
            }catch(Throwable e){
                log.warn(e.getMessage(),e);
            }
        }

    }

}
