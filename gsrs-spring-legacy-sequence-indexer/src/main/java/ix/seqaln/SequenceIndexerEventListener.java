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
            addToIndex(event.getOptionalFetchedEntityToReindex().get(), event.getEntityKey());
        }catch(Exception e) {
           log.warn("Trouble sequence indexing:" + event.getEntityKey(), e);
            
        }
    }

    @Async
    @TransactionalEventListener
    public void onCreate(IndexCreateEntityEvent event) {
        indexSequencesFor(event.getSource());
    }
    

//    private boolean couldHaveSequence(Key key) {
//       return key.getEntityInfo().couldHaveSequenceFields();
//    }

    private void indexSequencesFor(EntityUtils.Key source) {
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
                    //TODO: Ultimately, the indexing events themselves should come with fully
                    //qualified forms of the objects to be indexed, rather than requiring database
                    //key-based fetches ad-hoc (which happens before this). In most cases this 
                    //would prevent the need for any strange case where the type of sequence is 
                    //unknown (and has the added benefit that the indexing is faster). As it stands, 
                    //it's unfortunately common for the type of sequence not to be known at index 
                    //time. This leads to some strange situations.
                    
                    //The indexer is designed to know whether the indexed sequence is an NA or
                    //protein, and if it's not known at index time it won't actually be returned in
                    //later searches. 
                    
                    //While it's possible to know, for sure, that the sequence is a protein. It's 
                    //not possible to know it's a NA from the sequence alone, as most DNA sequences
                    //are also valid AA sequences.
                    
                    //For now, we attempt to just index all unknown sequences both ways
                    try {
                        indexer.add(k.getIdString(), ProteinSequence.of(
                                AminoAcid.cleanSequence(str.toString(), "X")));
                    }catch(Exception e) {
                        log.warn("problem indexing protein sequence:" + k.getIdString() + " with seq:" + str.toString());
                    }
                    try {
                        NucleotideSequence nts = NucleotideSequence.of(
                                Nucleotide.cleanSequence(str.toString(), "N"));
                        String nas=nts.toString();
                        String missingNs=nas.replace("N", "");
                        long fcount = nas.length();
                        long ncount = fcount-missingNs.length();
                        //if more than half of the residues are unknown, it's probably not actually
                        //an NA. Otherwise, just assume it is an NA.
                        if(ncount<fcount*0.5) {                          
                            indexer.add(k.getIdString(), nts);
                        }
                        
                    }catch(Exception e) {
                        log.warn("problem indexing nucleic acid sequence:" + k.getIdString() + " with seq:" + str.toString());
                    }
                  
                    
                    //This fallback is not well defined, so is currently omitted
                    //indexer.add(k.getIdString(), str.toString());
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
        if(!event.getSource().getEntityInfo().couldHaveSequenceFields()) {
            return;
        }
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
