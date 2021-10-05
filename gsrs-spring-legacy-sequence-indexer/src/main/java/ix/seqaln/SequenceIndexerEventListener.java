package ix.seqaln;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jcvi.jillion.core.residue.aa.AminoAcid;
import org.jcvi.jillion.core.residue.aa.ProteinSequence;
import org.jcvi.jillion.core.residue.nt.Nucleotide;
import org.jcvi.jillion.core.residue.nt.NucleotideSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
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
            addToIndex(event.getOptionalFetchedEntityToReindex().get(), event.getEntityKey());
        }catch(Exception e) {
           log.warn("Trouble sequence indexing:" + event.getEntityKey(), e);
            
        }
    }

    @TransactionalEventListener
    public void onCreate(IndexCreateEntityEvent event) {
        indexSequencesFor(event.getSource());
    }

    private void indexSequencesFor(EntityUtils.Key source) {
        try {
            
            Optional<EntityUtils.EntityWrapper<?>> opt= source.fetch();
            if(opt.isPresent()) {
                EntityUtils.EntityWrapper<?> ew = opt.get();
                if (!ew.isEntity() || !ew.hasKey()) {
                    return;
                }
                EntityUtils.Key k = ew.getKey();
                addToIndex(ew, k);
            }
        }catch(Exception e) {
           log.warn("Trouble sequence indexing:" + source, e);
            
        }
    }

    private void addToIndex(EntityUtils.EntityWrapper<?> ew, EntityUtils.Key k) {
        ew.streamSequenceFieldAndValues(d->true).map(p->p.v()).filter(s->s instanceof String).forEach(str->{
            try {
                boolean added=false;
                Object value = ew.getValue();
                if(value instanceof SequenceEntity){
                    SequenceEntity.SequenceType type = ((SequenceEntity)value).computeSequenceType();
                    if(type==SequenceEntity.SequenceType.NUCLEIC_ACID){
                        added=true;
                        indexer.add(k.getIdString(), NucleotideSequence.of(
                                Nucleotide.cleanSequence(str.toString(), "N")));
                    }else if(type==SequenceEntity.SequenceType.PROTEIN) {
                        added = true;
                        indexer.add(k.getIdString(), ProteinSequence.of(
                                AminoAcid.cleanSequence(str.toString(), "X")));
                    }

                }
                if(!added){
                    indexer.add(k.getIdString(), str.toString());
                }
            } catch (Exception e) {
                log.warn("Error indexing sequence", e);
            }
        });
    }

    @TransactionalEventListener
    public void onRemove(IndexRemoveEntityEvent event){
        EntityUtils.EntityWrapper ew = event.getSource();
        if(ew.isEntity() && ew.hasKey()) {
            removeFromIndex(ew, ew.getKey());
        }
    }

    @TransactionalEventListener
    public void onUpdate(IndexUpdateEntityEvent event){
        
        EntityUtils.EntityWrapper ew = event.getSource().fetch().get();
        if(ew.isEntity() && ew.hasKey()) {
            EntityUtils.Key key = ew.getKey();
            removeFromIndex(ew, key);
            addToIndex(ew, key);
        }
    }

    private void removeFromIndex(EntityUtils.EntityWrapper ew, EntityUtils.Key key) {

        ew.getEntityInfo().getSequenceFieldInfo().stream().findAny().ifPresent(s -> {
            GsrsSpringUtils.tryTaskAtMost(() -> indexer.remove(key.getIdString()), t -> t.printStackTrace(), 2);

        });
    }


}
