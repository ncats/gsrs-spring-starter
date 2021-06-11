package ix.core.search.text;

import gsrs.events.MaintenanceModeEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.indexer.IndexRemoveEntityEvent;
import gsrs.indexer.IndexUpdateEntityEvent;
import gsrs.springUtils.AutowireHelper;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.persistence.EntityManager;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Hibernate Entity listener that will update our legacy {@link TextIndexer}.
 */
@Component
public class TextIndexerEntityListener {

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    @Autowired
    private EntityManager em;

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
                Optional<EntityUtils.EntityWrapper<?>> opt = event.getSource().fetch(em);
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
        Optional<EntityUtils.EntityWrapper<?>> opt = event.getEntityKey().fetch(em);
        if(opt.isPresent()){
            textIndexerFactory.getDefaultInstance().add(opt.get());
        }
    }
    @EventListener
    public void reindexing(MaintenanceModeEvent event) {
        autowireIfNeeded();
            if(event.getSource().isInMaintenanceMode()){
                textIndexerFactory.getDefaultInstance().newProcess();

            }else{
                textIndexerFactory.getDefaultInstance().doneProcess();
            }
    }


    @EventListener
    public void updateEntity(IndexUpdateEntityEvent event) throws Exception {
//        System.out.println("updating index " + obj);
        autowireIfNeeded();
        TextIndexer indexer = textIndexerFactory.getDefaultInstance();
        if(indexer !=null) {
            EntityUtils.EntityWrapper ew = event.getSource();
            indexer.remove(ew);
            indexer.add(ew);
        }
    }
    @EventListener
    public void deleteEntity(IndexRemoveEntityEvent event) throws Exception {
//        System.out.println("removing from index " + obj);
        autowireIfNeeded();
        textIndexerFactory.getDefaultInstance().remove(event.getSource());
    }
}
