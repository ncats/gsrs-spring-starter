package ix.core.search.text;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
@Service
public class Lucene4IndexServiceFactory implements IndexerServiceFactory {
    @Override
    public IndexerService createInMemory() throws IOException {
        return new Lucene4IndexService();
    }

    @Override
    public IndexerService createForDir(File dir) throws IOException {
        return new Lucene4IndexService(dir);
    }
}
