package ix.core.search.text;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import gsrs.events.MaintenanceModeEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.indexer.IndexRemoveEntityEvent;
import gsrs.indexer.IndexUpdateEntityEvent;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import lombok.extern.slf4j.Slf4j;

/**
 * Hibernate Entity listener that will update our legacy {@link TextIndexer}.
 */
@Component
@Slf4j
@Transactional(readOnly = true)
public class TextIndexerEntityListener {
	   
    private HashSet<Key> working =new LinkedHashSet<>();
    
    // TP 08-27-2021: In general this shouldn't be necessary,
    // but if a new transactional operation inside a preUpdate
    // hook gets called, this sometimes tries to re-run all preUpdate
    // hooks at the "close" of that transaction. Setting proper isolation
    // levels should help prevent this, but this isn't always obvious.
    // At the time of this comment it's not necessary in any known code
    // to have this on. But it may turn out to be necessary.
    private static boolean PREVENT_RECURSION=true;
    
    
    @Autowired
    private TextIndexerFactory textIndexerFactory;

//    
//    @PersistenceContext(unitName =  DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
//    private EntityManager em;

    private void autowireIfNeeded(){
        if(textIndexerFactory==null) {
            AutowireHelper.getInstance().autowire(this);
        }
    }
    @Async
    @TransactionalEventListener
    public void created(IndexCreateEntityEvent event) throws Exception{
        autowireIfNeeded();
        try {
            TextIndexer indexer = textIndexerFactory.getDefaultInstance();
            if(indexer !=null) {
                
                //refetch from db
                Optional<EntityUtils.EntityWrapper> opt = EntityFetcher.of(event.getSource()).getIfPossible().map(m->EntityWrapper.of(m));
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
        Optional<EntityUtils.EntityWrapper<?>> opt = event.getOptionalFetchedEntityToReindex();
        
        if(opt.isPresent()){
        	if(event.isRequiresDelete()) {
        		textIndexerFactory.getDefaultInstance().update(opt.get());
        	}else {
        		textIndexerFactory.getDefaultInstance().add(opt.get());	
        	}
            
        }
    }
    @EventListener
    public void reindexing(MaintenanceModeEvent event) {
        autowireIfNeeded();
        //TODO: it shouldn't be the maintenance mode itself that triggers this
        // it should be a "WipeAllIndexes" event or something.
        if(event.getSource().isInMaintenanceMode()){
            textIndexerFactory.getDefaultInstance().newProcess();
        }else{
            textIndexerFactory.getDefaultInstance().doneProcess();
        }
    }
    

    @Async
    @TransactionalEventListener
    public void updateEntity(IndexUpdateEntityEvent event) {
        autowireIfNeeded();
        Key k = event.getSource();
        
        if(PREVENT_RECURSION) {
            if(working.contains(k)) {
                log.warn("Update TextIndexer called, but already updating record for:" + k);
                return;
            }
        }
        
        try {
            if(PREVENT_RECURSION) {
                working.add(k);
            }
            TextIndexer indexer = textIndexerFactory.getDefaultInstance();
            if(indexer !=null) {
                try {
                    EntityUtils.EntityWrapper ew = event.getOptionalFetchedEntity().orElse(null);
                    indexer.update(ew);
                }catch(Throwable t){
                    log.warn("trouble updating index for:" + event.getSource().toString(), t);
                }
            }
        } finally {
            if(PREVENT_RECURSION) {
                working.remove(k);
            }
        }
    }
    @Async
    @TransactionalEventListener
    public void deleteEntity(IndexRemoveEntityEvent event) throws Exception {
//        System.out.println("removing from index " + obj);
        autowireIfNeeded();
        textIndexerFactory.getDefaultInstance().remove(event.getSource());
    }
}
