package gsrs.legacy.structureIndexer;

import gov.nih.ncats.molwitch.Chemical;

import java.io.IOException;

public interface StructureIndexerService {
    void add(String id, String structure) throws IOException;
    void add(String id, Chemical structure) throws IOException;

    void remove(String id) throws IOException;

    void removeAll() throws IOException;
}
