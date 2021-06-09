package ix.seqaln.service;

import ix.seqaln.SequenceIndexer;
import org.jcvi.jillion.core.residue.aa.ProteinSequence;
import org.jcvi.jillion.core.residue.nt.NucleotideSequence;

import java.io.IOException;

public interface SequenceIndexerService {

    long getLastModified();

    void removeAll() throws IOException;

    void remove(String id) throws IOException;
    void add(String id, String sequence) throws IOException;

    void add(String id, NucleotideSequence sequence) throws IOException;

    void add(String id, ProteinSequence sequence) throws IOException;

    default SequenceIndexer.ResultEnumeration search(String query, double identity, SequenceIndexer.CutoffType rt, String seqType) {
        return search (query, identity, 1,rt, seqType);
    }

    SequenceIndexer.ResultEnumeration search (final String query,
                                                     final double identity, final int gap, SequenceIndexer.CutoffType rt, String seqType);
}
