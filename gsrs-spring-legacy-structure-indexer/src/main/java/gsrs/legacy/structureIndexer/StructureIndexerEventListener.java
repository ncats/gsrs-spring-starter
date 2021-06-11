package gsrs.legacy.structureIndexer;

import gsrs.events.MaintenanceModeEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.indexer.IndexRemoveEntityEvent;
import gsrs.indexer.IndexUpdateEntityEvent;
import gsrs.springUtils.GsrsSpringUtils;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class StructureIndexerEventListener {

    private final StructureIndexerService indexer;
    private final EntityManager em;


    private AtomicBoolean inMaintenanceMode = new AtomicBoolean(false);
    @Autowired
    public StructureIndexerEventListener(StructureIndexerService indexer, EntityManager em) {
        this.indexer = indexer;
        this.em = em;
    }

    @EventListener
    public void reindexing(MaintenanceModeEvent event) throws IOException {
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

        indexSequences(event.getEntityKey());
    }


    @TransactionalEventListener
    public void onCreate(IndexCreateEntityEvent event) {
        EntityUtils.Key key = event.getSource();
        indexSequences(key);
    }

    private void indexSequences(EntityUtils.Key key) {
        Optional<EntityUtils.EntityWrapper<?>> opt = key.fetch(em);
        if(opt.isPresent()) {
            EntityUtils.EntityWrapper<?> ew = opt.get();
            if (!ew.isEntity() || !ew.hasKey()) {
                return;
            }
            EntityUtils.Key k = ew.getKey();
            addToIndex(ew, k);

        }
    }

    private void addToIndex(EntityUtils.EntityWrapper<?> ew, EntityUtils.Key k) {
        ew.streamStructureFieldAndValues(d->true).map(p->p.v()).filter(s->s instanceof String).forEach(str-> {
            try {
                indexer.add(k.getIdString(), str.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @EventListener
    public void onRemove(IndexRemoveEntityEvent event){
        EntityUtils.EntityWrapper ew = event.getSource();
        EntityUtils.Key key = ew.getKey();
        removeFromIndex(ew,key);
    }

    @EventListener
    public void onUpdate(IndexUpdateEntityEvent event){
        EntityUtils.EntityWrapper ew = event.getSource();
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
