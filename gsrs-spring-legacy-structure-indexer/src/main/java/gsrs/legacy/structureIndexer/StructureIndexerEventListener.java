package gsrs.legacy.structureIndexer;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

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

    private boolean couldHaveStructures(Key key) {
        boolean couldHaveStructure = key.getEntityInfo()
//              .getInherittedRootEntityInfo()
              .getTypeAndSubTypes()
              .stream()
              .map(tt->tt.getStructureFieldInfo())
              .filter(tt->tt!=null)
              .anyMatch(tt->!tt.isEmpty())
              ;
        return couldHaveStructure;
    }
    
    private void indexStructures(EntityUtils.Key key) {
        try {
           
            if(!couldHaveStructures(key)) {
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
        ew.streamStructureFieldAndValues(d->true).map(p->p.v()).filter(s->s instanceof String).forEach(str-> {
            try {
                indexer.add(k.getIdString(), str.toString());
            } catch (Throwable e) {
                log.warn("Trouble adding structure to index:" + k.toString(), e);
            }
        });
    }
    
    @Async
    @TransactionalEventListener
    public void onRemove(IndexRemoveEntityEvent event){
   
        EntityUtils.EntityWrapper ew = event.getSource();
        EntityUtils.Key key = ew.getKey();
        if(!couldHaveStructures(key)) {
            return;
        }
        removeFromIndex(ew,key);
    }

    @Async
    @TransactionalEventListener
    public void onUpdate(IndexUpdateEntityEvent event){
        Key k = event.getSource();
        if(!couldHaveStructures(k)) {
            return;
        }
        EntityUtils.EntityWrapper ew = (useExplicitEM)?k.fetch(em).get():k.fetch().get();
        if(ew.isEntity() && ew.hasKey()) {
            EntityUtils.Key key = ew.getKey();
            removeFromIndex(ew, key);
            addToIndex(ew, key);
        }
    }

    private void removeFromIndex(EntityUtils.EntityWrapper ew, EntityUtils.Key key) {

        ew.getEntityInfo().getStructureFieldInfo().stream().findAny().ifPresent(s -> {
            GsrsSpringUtils.tryTaskAtMost(() -> indexer.remove(key.getIdString()), t -> t.printStackTrace(), 2);
        });
    }


}
