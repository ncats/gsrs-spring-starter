package gsrs.legacy.structureIndexer;

import gov.nih.ncats.common.io.IOUtil;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.structureIndexer.StructureIndexer;
import gsrs.springUtils.StaticContextAccessor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;

@Service
public class LegacyStructureIndexerService implements StructureIndexerService{

    private StandardizedStructureIndexer indexer;
    private File dir;

    @Autowired
    public LegacyStructureIndexerService(@Value("${ix.structure.base}") File dir) throws IOException {
        IOUtil.mkdirs(dir);
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

    
    //Due to the order of shutdowns / startups that happens in the current
    //setup for the services, this service is typically one of the first services
    //loaded and last services unloaded. Because of this, the @PreDestroy hook
    //happens after some important mechanisms, like deleting files in the tests.
    //When this happens it can result in distracting IOException stacktraces.
    //However, if a StaticContextAccessor shutdownhook is used instead, the shutdown operation
    //is called earlier and the IOExceptions can be avoided.
    @PostConstruct
    public void setupShutdownHook() {
        StandardizedStructureIndexer sindexer=indexer;
        StaticContextAccessor.addStaticShutdownRunnable(()->{
            sindexer.shutdown();
        });
    }
//    
    @PreDestroy
    @Override
    public void shutdown(){
        //Not used due to order of operations issues
//        indexer.shutdown();
    }

    @Override
    public StructureIndexer.ResultEnumeration substructure(String query) throws Exception {
         return indexer.substructure(query,0);
    }

    @Override
    public StructureIndexer.ResultEnumeration similarity(String query, double threshold) throws Exception {
        return indexer.similarity(query,threshold);
    }



    @Override
    public long lastModified(){
        return indexer.lastModified();
    }
}
