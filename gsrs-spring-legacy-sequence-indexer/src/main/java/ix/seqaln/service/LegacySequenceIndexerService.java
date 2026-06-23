package ix.seqaln.service;

import java.io.File;
import java.io.IOException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jcvi.jillion.core.residue.aa.ProteinSequence;
import org.jcvi.jillion.core.residue.nt.NucleotideSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gsrs.springUtils.StaticContextAccessor;
import ix.seqaln.SequenceIndexer;
import ix.seqaln.configuration.LegacySequenceAlignmentConfiguration;

@Service
public class LegacySequenceIndexerService implements SequenceIndexerService {

    private File dir;

    private LegacySequenceAlignmentConfiguration configuration;


    private SequenceIndexer indexer;

    @Autowired
    public LegacySequenceIndexerService(LegacySequenceAlignmentConfiguration configuration, @Value("${ix.sequence.base}") File dir) throws IOException {
        this.configuration = configuration;
        this.dir = dir;
        indexer = SequenceIndexer.open(dir);
        indexer.setKmerSize(configuration.getDefaultKmer());
        indexer.setNucleicKmer(configuration.getNuc());
        indexer.setProteinKmer(configuration.getProt());
    }

    @Override
    public void removeAll() throws IOException {
        indexer.removeAll();
    }

    @Override
    public void remove(String id) throws IOException{
        indexer.remove(id);
    }

    @Override
    public long getLastModified() {
        return indexer.lastModified();
    }

    @Override
    public void add(String id, String sequence) throws IOException {
        indexer.add(id, sequence);
    }

    @Override
    public void add(String id, NucleotideSequence sequence) throws IOException {
        indexer.add(id, sequence);
    }

    @Override
    public void add(String id, ProteinSequence sequence) throws IOException {
        indexer.add(id, sequence);
    }

    @Override
    public SequenceIndexer.ResultEnumeration search(String query, double identity, int gap, SequenceIndexer.CutoffType rt, String seqType) {

        return indexer.search(query, identity, gap, rt, seqType);
    }

    
    
    //Due to the order of shutdowns / startups that happens in the current
    //setup for the services, this service is typically one of the first services
    //loaded and last services unloaded. Because of this, the @PreDestroy hook
    //happens after some important mechanisms, like deleting files in the tests.
    //When this happens it can result in distracting IOException stacktraces.
    //However, if a StaticContextAccessor shutdownhook is used instead, the shutdown operation
    //is called earlier and the IOExceptions can be avoided.
    @PostConstruct
    public void setupShutdownHook() {
        SequenceIndexer sindexer=indexer;
        StaticContextAccessor.addStaticShutdownRunnable(()->{
            sindexer.shutdown();
        });
    }
    
    
//    @PreDestroy
//    public void shutdown(){
////        actualShutdown();
//    }
    
}
