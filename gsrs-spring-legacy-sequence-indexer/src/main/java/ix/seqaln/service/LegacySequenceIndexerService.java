package ix.seqaln.service;

import ix.seqaln.SequenceIndexer;
import ix.seqaln.configuration.LegacySequenceAlignmentConfiguration;
import org.jcvi.jillion.core.residue.aa.ProteinSequence;
import org.jcvi.jillion.core.residue.nt.NucleotideSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;

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

    @PreDestroy
    public void shutdown(){
        indexer.shutdown();
    }
}
