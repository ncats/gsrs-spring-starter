package ix.seqaln;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import gsrs.sequence.indexer.SequenceEntityIndexCreateEvent;
import gsrs.sequence.indexer.SequenceEntityUpdateCreateEvent;
import org.jcvi.jillion.core.residue.aa.AminoAcid;
import org.jcvi.jillion.core.residue.aa.ProteinSequence;
import org.jcvi.jillion.core.residue.nt.Nucleotide;
import org.jcvi.jillion.core.residue.nt.NucleotideSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.JsonNode;

import gsrs.DataSourceConfigRegistry;
import gsrs.DefaultDataSourceConfig;
import gsrs.events.MaintenanceModeEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.indexer.IndexRemoveEntityEvent;
import gsrs.indexer.IndexUpdateEntityEvent;
import gsrs.springUtils.GsrsSpringUtils;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.models.SequenceEntity;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.Key;
import ix.seqaln.service.SequenceIndexerService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SequenceIndexerEventListener {

    private final SequenceIndexerService indexer;
//    private EntityManager em;
    
//    @PersistenceContext(unitName =  DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
//    private EntityManager em;


    private AtomicBoolean inMaintenanceMode = new AtomicBoolean(false);
    @Autowired
    public SequenceIndexerEventListener(
            SequenceIndexerService indexer) {

        this.indexer = indexer;
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
    public void reindexingEntity(ReindexEntityEvent event) throws IOException {       
        try {
            addToIndex(event.getOptionalFetchedEntityToReindex().get(), event.getEntityKey(),null);
        }catch(Exception e) {
           log.warn("Trouble sequence indexing:" + event.getEntityKey(), e);
            
        }
    }

    @Async
    @TransactionalEventListener
    public void onCreate(IndexCreateEntityEvent event) {

        if(event instanceof SequenceEntityIndexCreateEvent)return;
        indexSequencesFor(event.getSource(),null);
    }

    @Async
    @TransactionalEventListener
    public void onCreate(SequenceEntityIndexCreateEvent event) {
        indexSequencesFor(event.getSource(), event.getSequenceType());
    }

//    private boolean couldHaveSequence(Key key) {
//       return key.getEntityInfo().couldHaveSequenceFields();
//    }

    private void indexSequencesFor(EntityUtils.Key source, SequenceEntity.SequenceType sequenceType) {
        try {
            if(!source.getEntityInfo().couldHaveSequenceFields()) {
                return;
            }
            Optional<EntityUtils.EntityWrapper<?>> opt= source.fetch();
            if(opt.isPresent()) {
                EntityUtils.EntityWrapper<?> ew = opt.get();
                if (!ew.isEntity() || !ew.hasKey()) {
                    return;
                }
                EntityUtils.Key k = ew.getKey();
                addToIndex(ew, k, sequenceType);
            }
        }catch(Exception e) {
           log.warn("Trouble sequence indexing:" + source, e);
            
        }
    }

    private void addToIndex(EntityUtils.EntityWrapper<?> ew, EntityUtils.Key k, SequenceEntity.SequenceType sequenceType) {
        ew.streamSequenceFieldAndValues(d->true).map(p->p.v()).filter(s->s instanceof String).forEach(str->{
            try {
                boolean added=false;
                Object value = ew.getValue();
                SequenceEntity.SequenceType type=sequenceType;
                if(type ==null && value instanceof SequenceEntity){
                    type = ((SequenceEntity)value).computeSequenceType();

                }
                if(type==SequenceEntity.SequenceType.NUCLEIC_ACID){
                        added=true;
                        indexer.add(k.getIdString(), NucleotideSequence.of(
                                Nucleotide.cleanSequence(str.toString(), "N")));
                }else if(type==SequenceEntity.SequenceType.PROTEIN) {
                    added = true;
                    indexer.add(k.getIdString(), ProteinSequence.of(
                            AminoAcid.cleanSequence(str.toString(), "X")));
                }


                if(!added){
                    indexer.add(k.getIdString(), str.toString());
                }
            } catch (Exception e) {
                log.warn("Error indexing sequence", e);
            }
        });
    }

    @Async
    @TransactionalEventListener
    public void onRemove(IndexRemoveEntityEvent event){
        EntityUtils.EntityWrapper ew = event.getSource();
        if(ew.isEntity() && ew.hasKey()) {
            if(!ew.getKey().getEntityInfo().couldHaveSequenceFields()) {
                return;
            }
            removeFromIndex(ew, ew.getKey());
        }
    }

    @Async
    @TransactionalEventListener
    public void onUpdate(IndexUpdateEntityEvent event){
        if(event instanceof SequenceEntityUpdateCreateEvent)return;
        if(!event.getSource().getEntityInfo().couldHaveSequenceFields()) {
            return;
        }
        EntityUtils.EntityWrapper ew = event.getSource().fetch().get();
        if(ew.isEntity() && ew.hasKey()) {
            EntityUtils.Key key = ew.getKey();
            removeFromIndex(ew, key);
            addToIndex(ew, key,null);
        }
    }
    @Async
    @TransactionalEventListener
    public void onUpdate(SequenceEntityUpdateCreateEvent event){
        if(!event.getSource().getEntityInfo().couldHaveSequenceFields()) {
            return;
        }
        EntityUtils.EntityWrapper ew = event.getSource().fetch().get();
        if(ew.isEntity() && ew.hasKey()) {
            EntityUtils.Key key = ew.getKey();
            removeFromIndex(ew, key);
            addToIndex(ew, key, event.getSequenceType());
        }
    }

    private void removeFromIndex(EntityUtils.EntityWrapper ew, EntityUtils.Key key) {

        ew.getEntityInfo().getSequenceFieldInfo().stream().findAny().ifPresent(s -> {
            GsrsSpringUtils.tryTaskAtMost(() -> indexer.remove(key.getIdString()), t -> t.printStackTrace(), 2);

        });
    }


}
