package gsrs.legacy.structureIndexer;

import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.structureIndexer.StructureIndexer;

import java.io.IOException;

public interface StructureIndexerService {
    void add(String id, String structure) throws IOException;
    void add(String id, Chemical structure) throws IOException;

    void remove(String id) throws IOException;

    void removeAll() throws IOException;

    StructureIndexer.ResultEnumeration substructure(String query) throws Exception;

    StructureIndexer.ResultEnumeration similarity(String query, double threshold) throws Exception;
}
