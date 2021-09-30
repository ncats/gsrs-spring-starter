package ix.core.search.text;

import gsrs.DefaultDataSourceConfig;
import gsrs.events.BeginReindexEvent;
import gsrs.events.EndReindexEvent;
import gsrs.events.MaintenanceModeEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.indexer.IndexRemoveEntityEvent;
import gsrs.indexer.IndexUpdateEntityEvent;
import gsrs.springUtils.AutowireHelper;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hibernate Entity listener that will update our legacy {@link TextIndexer}.
 */
@Component
@Transactional(readOnly = true)
public class TextIndexerEntityListener {

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    private Set<UUID> reindexEventUUIDs = Collections.newSetFromMap(new ConcurrentHashMap<>());
//    
//    @PersistenceContext(unitName =  DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
//    private EntityManager em;

    private void autowireIfNeeded(){
        if(textIndexerFactory==null) {
            AutowireHelper.getInstance().autowire(this);
        }
    }

    @TransactionalEventListener
    public void created(IndexCreateEntityEvent event) throws Exception{
        autowireIfNeeded();
        try {
            TextIndexer indexer = textIndexerFactory.getDefaultInstance();
            if(indexer !=null) {
                
                //refetch from db
                Optional<EntityUtils.EntityWrapper<?>> opt = event.getSource().fetch();
                if(opt.isPresent()) {
                    EntityUtils.EntityWrapper ew = opt.get();

                    if (event.shouldDeleteFirst()) {
                        indexer.remove(ew);
                    }
                    indexer.add(ew);
                }
            }
        } catch (Throwable e) {
            
            throw new Exception(e);
        }
    }
    @EventListener
    public void reindexEntity(ReindexEntityEvent event) throws IOException {
        autowireIfNeeded();
        Optional<EntityUtils.EntityWrapper<?>> opt = event.getEntityKey().fetch();
        if(opt.isPresent()){
            textIndexerFactory.getDefaultInstance().add(opt.get());
        }
    }
    @EventListener
    public void doneReindexing(EndReindexEvent event) {
        if(reindexEventUUIDs.remove(event.getId())) {
            textIndexerFactory.getDefaultInstance().doneProcess();
        }
    }
    @EventListener
    public void reindexing(BeginReindexEvent event) {
        autowireIfNeeded();
        reindexEventUUIDs.add(event.getId());
        switch(event.getWipeIndexStrategy()){
            case CLEAR_ALL:
                textIndexerFactory.getDefaultInstance().newProcess();
                break;
            case REMOVE_ONLY_SPECIFIED_TYPES:
                for(String t: event.getTypesToClearFromIndex()) {
                    try {
                        textIndexerFactory.getDefaultInstance().beginPartialReindex();
                        textIndexerFactory.getDefaultInstance().removeAllType(EntityUtils.getEntityInfoFor(t));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;
        }


    }


    @TransactionalEventListener
    public void updateEntity(IndexUpdateEntityEvent event) throws Exception {
//        System.out.println("updating index " + obj);
        autowireIfNeeded();
        TextIndexer indexer = textIndexerFactory.getDefaultInstance();
        if(indexer !=null) {
            try {
                EntityUtils.EntityWrapper ew = event.getSource().fetch().get();
                indexer.update(ew);
            }catch(Throwable t){
                t.printStackTrace();
            }
        }
    }
    @TransactionalEventListener
    public void deleteEntity(IndexRemoveEntityEvent event) throws Exception {
//        System.out.println("removing from index " + obj);
        autowireIfNeeded();
        textIndexerFactory.getDefaultInstance().remove(event.getSource());
    }
}
