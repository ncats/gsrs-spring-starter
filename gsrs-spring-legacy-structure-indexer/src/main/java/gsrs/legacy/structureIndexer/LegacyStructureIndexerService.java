package gsrs.legacy.structureIndexer;

import gov.nih.ncats.common.io.IOUtil;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.structureIndexer.StructureIndexer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;

@Service
public class LegacyStructureIndexerService implements StructureIndexerService{

    private StandardizedStructureIndexer indexer;
    private File dir;
    @Autowired
    public LegacyStructureIndexerService(@Value("${ix.structure.base}") File dir) throws IOException {
        indexer =  new StandardizedStructureIndexer(StructureIndexer.open(dir));
        this.dir = dir;
    }

    @Override
    public void removeAll() throws IOException {
        //it's easier to shutdown delete and reopen
        indexer.shutdown();

        IOUtil.deleteRecursivelyQuitely(dir);
        indexer =  new StandardizedStructureIndexer(StructureIndexer.open(dir));
    }

    @Override
    public void add(String id, Chemical structure) throws IOException {
        indexer.add(id, structure);
    }
    @Override
    public void add(String id, String structure) throws IOException {
        indexer.add(id, structure);
    }

    @Override
    public void remove(String id) throws IOException {
        indexer.remove(null, id);
    }
    @PreDestroy
    public void shutdown(){
        indexer.shutdown();
    }
}
