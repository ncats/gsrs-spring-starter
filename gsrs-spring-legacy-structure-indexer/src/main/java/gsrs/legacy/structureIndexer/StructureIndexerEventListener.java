package gsrs.legacy.structureIndexer;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import jakarta.persistence.EntityManager;

import com.google.common.util.concurrent.Striped;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import gsrs.DefaultDataSourceConfig;
import gsrs.events.MaintenanceModeEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.indexer.IndexRemoveEntityEvent;
import gsrs.indexer.IndexUpdateEntityEvent;
import gsrs.springUtils.GsrsSpringUtils;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.Key;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StructureIndexerEventListener {

    private final StructureIndexerService indexer;


    private EntityManager em;
    
    private boolean useExplicitEM =false;

    private Striped<Lock> stripedLock = Striped.lazyWeakLock(8);

    private AtomicBoolean inMaintenanceMode = new AtomicBoolean(false);
    @Autowired
    public StructureIndexerEventListener(StructureIndexerService indexer,
            @Qualifier(DefaultDataSourceConfig.NAME_ENTITY_MANAGER)  EntityManager em
            ) {
        this.indexer = indexer;
        this.em = em;
        
    }
    
    public void useExplicitEM(boolean b) {
        useExplicitEM=b;
    }

    @EventListener
    public void reindexing(MaintenanceModeEvent event) throws IOException {
        //TODO: it shouldn't be the maintenance mode itself that triggers this
        // it should be a "WipeAllIndexes" event or something.
        if(event.getSource().isInMaintenanceMode()){
            //begin
            indexer.removeAll();
            inMaintenanceMode.set(true);
        }else{
            inMaintenanceMode.set(false);
        }

    }

    @EventListener
    public void reindexEntity(ReindexEntityEvent event){
        try {
        	if(event.isRequiresDelete()) {
        		 removeFromIndex(event.getOptionalFetchedEntityToReindex().get(), event.getEntityKey());
         	}
            addToIndex(event.getOptionalFetchedEntityToReindex().get(), event.getEntityKey());
        }catch(Exception e) {
           log.warn("Trouble structure indexing:" + event.getEntityKey(), e);
            
        }
    }

    @Async
    @TransactionalEventListener
    public void onCreate(IndexCreateEntityEvent event) {
        EntityUtils.Key key = event.getSource();
        indexStructures(key);
    }

    
    private void indexStructures(EntityUtils.Key key) {
        try {
           
            if(!key.getEntityInfo().couldHaveStructureFields()) {
                return;
            }
            Optional<EntityUtils.EntityWrapper<?>> opt= (useExplicitEM)?key.fetch(em):key.fetch();            

            if(opt.isPresent()) {
                EntityUtils.EntityWrapper<?> ew = opt.get();
                if (!ew.isEntity() || !ew.hasKey()) {
                    return;
                }
                EntityUtils.Key k = ew.getKey();
                addToIndex(ew, k);

            }
        }catch(Throwable e) {
            log.warn("Trouble structure indexing:" + key, e);
        }
    }

    private void addToIndex(EntityUtils.EntityWrapper<?> ew, EntityUtils.Key k) {
        Lock l = stripedLock.get(k);
        l.lock();
        try {
            ew.streamStructureFieldAndValues(d -> true).map(p -> p.v()).filter(s -> s instanceof String).forEach(str -> {
                try {
                    indexer.add(k.getIdString(), str.toString());
                } catch (Throwable e) {
                    log.warn("Trouble adding structure to index:" + k.toString(), e);
                }
            });
        }finally{
            l.unlock();
        }
    }
    
    @Async
    @TransactionalEventListener
    public void onRemove(IndexRemoveEntityEvent event){
   
        EntityUtils.EntityWrapper ew = event.getSource();
        EntityUtils.Key key = ew.getKey();
        if(!key.getEntityInfo().couldHaveStructureFields()) {
            return;
        }
        removeFromIndex(ew,key);
    }

    @Async
    @TransactionalEventListener
    public void onUpdate(IndexUpdateEntityEvent event){
        Key k = event.getSource();
        if(!k.getEntityInfo().couldHaveStructureFields()) {
            return;
        }
        EntityUtils.EntityWrapper ew = (useExplicitEM)?k.fetch(em).get():k.fetch().get();
        if(ew.isEntity() && ew.hasKey()) {
            EntityUtils.Key key = ew.getKey();
            Lock l = stripedLock.get(k);
            l.lock();
            try {
                removeFromIndex(ew, key);
                addToIndex(ew, key);
            }finally{
                l.unlock();
            }
        }
    }

    private void removeFromIndex(EntityUtils.EntityWrapper ew, EntityUtils.Key key) {
        Lock l = stripedLock.get(key);
        l.lock();
        try {
            ew.getEntityInfo().getStructureFieldInfo().stream().findAny().ifPresent(s -> {
                GsrsSpringUtils.tryTaskAtMost(() -> indexer.remove(key.getIdString()), t -> t.printStackTrace(), 2);
            });
        }finally{
            l.unlock();
        }
    }


}
