package ix.seqaln;

import gsrs.events.MaintenanceModeEvent;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.indexer.IndexRemoveEntityEvent;
import gsrs.indexer.IndexUpdateEntityEvent;
import gsrs.springUtils.GsrsSpringUtils;
import ix.core.models.SequenceEntity;
import ix.core.util.EntityUtils;
import ix.seqaln.service.SequenceIndexerService;
import org.jcvi.jillion.core.residue.aa.AminoAcid;
import org.jcvi.jillion.core.residue.aa.ProteinSequence;
import org.jcvi.jillion.core.residue.nt.Nucleotide;
import org.jcvi.jillion.core.residue.nt.NucleotideSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SequenceIndexerEventListener {

    private final SequenceIndexerService indexer;
    @Autowired
    public SequenceIndexerEventListener(SequenceIndexerService indexer) {
        this.indexer = indexer;
    }

    @EventListener
    public void reindexing(MaintenanceModeEvent event) throws IOException {
        if(event.getSource().isInMaintenanceMode()){
            //begin
            indexer.removeAll();
        }
    }


        @EventListener
    public void onCreate(IndexCreateEntityEvent event) {
        EntityUtils.EntityWrapper<?> ew = event.getSource();
        if(!ew.isEntity() || !ew.hasKey()){
            return;
        }
        EntityUtils.Key k = ew.getKey();
        addToIndex(ew, k);
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
                e.printStackTrace();
            }
        });
    }

    @EventListener
    public void onRemove(IndexRemoveEntityEvent event){
        EntityUtils.EntityWrapper ew = event.getSource();
        if(ew.isEntity() && ew.hasKey()) {
            removeFromIndex(ew, ew.getKey());
        }
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

        ew.getEntityInfo().getSequenceFieldInfo().stream().findAny().ifPresent(s -> {
            GsrsSpringUtils.tryTaskAtMost(() -> indexer.remove(key.getIdString()), t -> t.printStackTrace(), 2);

        });
    }


}
