package ix.seqaln;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.util.concurrent.Striped;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.sequence.SequenceFileSupport;
import gsrs.sequence.indexer.SequenceEntityIndexCreateEvent;
import gsrs.sequence.indexer.SequenceEntityUpdateCreateEvent;
import ix.core.models.Indexable;
import org.jcvi.jillion.core.residue.aa.AminoAcid;
import org.jcvi.jillion.core.residue.aa.ProteinSequence;
import org.jcvi.jillion.core.residue.nt.Nucleotide;
import org.jcvi.jillion.core.residue.nt.NucleotideSequence;
import org.jcvi.jillion.fasta.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import gsrs.events.MaintenanceModeEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.indexer.IndexRemoveEntityEvent;
import gsrs.indexer.IndexUpdateEntityEvent;
import gsrs.springUtils.GsrsSpringUtils;
import ix.core.models.SequenceEntity;
import ix.core.util.EntityUtils;
import ix.seqaln.service.SequenceIndexerService;
import lombok.extern.slf4j.Slf4j;

/**
 * A Spring Event Listener that will listen
 * to the events having to do with Nucleic Acid and Protein sequences.
 * Currently as of GSRS 3.0, this includes
 * entities whose {@link ix.core.models.Indexable} annotated fields
 * have {@link Indexable#sequence()} set to {@code true}
 * and entities who implements {@link SequenceFileSupport}.
 */
@Component
@Slf4j
public class SequenceIndexerEventListener {

    private final SequenceIndexerService indexer;

    private Striped<Lock> stripedLock = Striped.lazyWeakLock(8);


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
        EntityUtils.Key entityKey = event.getEntityKey();
        try {
            EntityUtils.EntityWrapper<?> ew = event.getOptionalFetchedEntityToReindex().get();
            if(event.isRequiresDelete()) {
            	removeFromIndex(ew, entityKey);
        	}
        	addSequenceFieldDataToIndex(ew, entityKey,null);
            addSequenceFileDataToIndex(entityKey, CachedSupplier.of(()->Optional.of(ew) ));
            
        }catch(Exception e) {
           log.warn("Trouble sequence indexing:" + entityKey, e);
            
        }
    }

    @Async
    @TransactionalEventListener
    public void onCreate(IndexCreateEntityEvent event) {

        if(event instanceof SequenceEntityIndexCreateEvent){
            indexSequencesFor(event.getSource(),event::getOptionalFetchedEntityToIndex, ((SequenceEntityIndexCreateEvent)event).getSequenceType());
        }else {
            indexSequencesFor(event.getSource(),event::getOptionalFetchedEntityToIndex, null);
        }
    }

    private void indexSequencesFor(EntityUtils.Key source, Supplier<Optional<EntityUtils.EntityWrapper<?>>> supplier, SequenceEntity.SequenceType sequenceType) {
        try {
            //fetch might be an expensive operation so wrap it in a cachedSupplier
            //since we might call this more than once now
            CachedSupplier<Optional<EntityUtils.EntityWrapper<?>>> entitySupplier = CachedSupplier.of(supplier);

            if(source.getEntityInfo().couldHaveSequenceFields()) {
                Optional<EntityUtils.EntityWrapper<?>> opt =entitySupplier.get();
                if(opt.isPresent()) {
                    EntityUtils.EntityWrapper<?> ew = opt.get();
                    if (ew.isEntity()){
                        Optional<EntityUtils.Key> optKey = ew.getOptionalKey();
                        if(optKey.isPresent()) {
                            EntityUtils.Key k = ew.getKey();
                            addSequenceFieldDataToIndex(ew, k, sequenceType);
                        }

                    }
                }
            }
            addSequenceFileDataToIndex(source, entitySupplier);

        }catch(Exception e) {
           log.warn("Trouble sequence indexing:" + source, e);
            
        }
    }

    /**
     * Add any sequences from referenced sequence files.  This is where those
     * sequence files are parsed.
     * @param source
     * @param entitySupplier
     */
    private void addSequenceFileDataToIndex(EntityUtils.Key source, CachedSupplier<Optional<EntityUtils.EntityWrapper<?>>> entitySupplier) {
       if(SequenceFileSupport.class.isAssignableFrom(source.getEntityInfo().getEntityClass())){
            Optional<EntityUtils.EntityWrapper<?>> opt = entitySupplier.get();
            if(opt.isPresent()) {
                EntityUtils.EntityWrapper<?> ew = opt.get();
                Lock l = stripedLock.get(ew.getKey());
                l.lock();
                try {
                    SequenceFileSupport sequenceFileSupport = (SequenceFileSupport) ew.getValue();
                    Object objectId = ew.getId().get();
                    try (Stream<SequenceFileSupport.SequenceFileData> stream = sequenceFileSupport.getSequenceFileData()) {
                        stream
                                .forEach(sfd -> {
                                    //in unlikely event there are different types in different files
                                    SequenceEntity.SequenceType sequenceTypeForFile = sfd.getSequenceType();
                                    //TODO if we add more file types change this to a switch or some kind of factory type lookup
                                    //to convert from a type into something that knows how to parse the file
                                    if (SequenceFileSupport.SequenceFileData.SequenceFileType.FASTA == sfd.getSequenceFileType()) {
                                        String fastaFilename = sfd.getName();
                                        try (InputStream in = sfd.createInputStream()) {
                                            FastaParser fastaParser = FastaFileParser.create(in);
                                            fastaParser.parse(new FastaVisitor() {
                                                @Override
                                                public FastaRecordVisitor visitDefline(FastaVisitorCallback fastaVisitorCallback, String id, String comment) {
                                                    //TODO process comments
                                                    return new AbstractFastaRecordVisitor(id, comment) {
                                                        @Override
                                                        protected void visitRecord(String id, String comment, String seq) {


                                                            try {
                                                                if (sequenceTypeForFile == SequenceEntity.SequenceType.NUCLEIC_ACID) {

                                                                    indexer.add(">" + objectId + "|" +fastaFilename+"|"+ id, NucleotideSequence.of(
                                                                            Nucleotide.cleanSequence(seq, "N")));
                                                                } else if (sequenceTypeForFile == SequenceEntity.SequenceType.PROTEIN) {

                                                                    indexer.add(">" + objectId + "|" +fastaFilename+"|"+ id, ProteinSequence.of(
                                                                            AminoAcid.cleanSequence(seq, "X")));
                                                                } else {
                                                                    indexer.add(">" + objectId + "|" +fastaFilename+"|"+ id, seq);
                                                                }
                                                            } catch (IOException e) {
                                                                log.warn("Trouble FASTA sequence indexing:" + id, e);   
                                                            }


                                                        }
                                                    };
                                                }

                                                @Override
                                                public void visitEnd() {

                                                }

                                                @Override
                                                public void halted() {

                                                }
                                            });


                                        } catch (IOException e) {
                                            log.warn("Trouble FASTA sequence indexing:" + source.toString(), e);   
                                        }
                                    }

                                });
                    }
                }finally{
                    l.unlock();
                }
            }
        }
    }

    private void addSequenceFieldDataToIndex(EntityUtils.EntityWrapper<?> ew, EntityUtils.Key k, SequenceEntity.SequenceType sequenceType) {
        Lock l = stripedLock.get(ew.getKey());
        l.lock();
        try {
            ew.streamSequenceFieldAndValues(d -> true).map(p -> p.v()).filter(s -> s instanceof String).forEach(str -> {
                try {
                    boolean added = false;
                    Object value = ew.getValue();
                    SequenceEntity.SequenceType type = sequenceType;
                    if (type == null && value instanceof SequenceEntity) {
                        type = ((SequenceEntity) value).computeSequenceType();

                    }
                    if (type == SequenceEntity.SequenceType.NUCLEIC_ACID) {
                        added = true;
                        indexer.add(k.getIdString(), NucleotideSequence.of(
                                Nucleotide.cleanSequence(str.toString(), "N")));
                    } else if (type == SequenceEntity.SequenceType.PROTEIN) {
                        added = true;
                        indexer.add(k.getIdString(), ProteinSequence.of(
                                AminoAcid.cleanSequence(str.toString(), "X")));
                    }


                    if (!added) {
                        indexer.add(k.getIdString(), str.toString());
                    }
                } catch (Exception e) {
                    log.warn("Error indexing sequence", e);
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
        //TODO: this is potentially inefficient, could use cached fetcher
        EntityUtils.EntityWrapper ew = event.getSource().fetch().get();
        if(ew.isEntity() && ew.hasKey()) {
            EntityUtils.Key key = ew.getKey();
            Lock l = stripedLock.get(ew.getKey());
            l.lock();
            try {
                removeFromIndex(ew, key);
                addSequenceFieldDataToIndex(ew, key, null);
            }finally{
                l.unlock();
            }
        }
    }
    @Async
    @TransactionalEventListener
    public void onUpdate(SequenceEntityUpdateCreateEvent event){
        if(!event.getSource().getEntityInfo().couldHaveSequenceFields()) {
            return;
        }
        //TODO: this is potentially inefficient, could use cached fetcher
        EntityUtils.EntityWrapper ew = event.getSource().fetch().get();
        if(ew.isEntity() && ew.hasKey()) {
            EntityUtils.Key key = ew.getKey();
            removeFromIndex(ew, key);
            addSequenceFieldDataToIndex(ew, key, event.getSequenceType());
        }
    }

    private void removeFromIndex(EntityUtils.EntityWrapper ew, EntityUtils.Key key) {
        Lock l = stripedLock.get(key);
        l.lock();
        try {
            ew.getEntityInfo().getSequenceFieldInfo().stream().findAny().ifPresent(s -> {
                GsrsSpringUtils.tryTaskAtMost(() -> indexer.remove(key.getIdString()), t -> t.printStackTrace(), 2);

            });
        }finally{
            l.unlock();
        }
    }


}
