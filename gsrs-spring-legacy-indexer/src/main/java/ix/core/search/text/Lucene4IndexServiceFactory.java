package ix.core.search.text;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.search.BooleanQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Lucene4IndexServiceFactory implements IndexerServiceFactory {
	
	@Value("${lucene.BooleanQuery.MaxClauseCount:2048}")
	private int maxClauseCount;
	
    @Override
    public IndexerService createInMemory() throws IOException {
    	BooleanQuery.setMaxClauseCount(maxClauseCount);    	
        return new Lucene4IndexService();
    }

    @Override
    public IndexerService createForDir(File dir) throws IOException {
    	BooleanQuery.setMaxClauseCount(maxClauseCount);    	
        return new Lucene4IndexService(dir);
    }
}
