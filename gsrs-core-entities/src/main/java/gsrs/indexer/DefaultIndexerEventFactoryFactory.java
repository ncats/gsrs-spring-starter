package gsrs.indexer;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ix.core.util.EntityUtils.Key;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Data
public class DefaultIndexerEventFactoryFactory implements IndexerEventFactoryFactory{
    @Autowired(required = false)
    private List<IndexerEventFactory> factories = Arrays.asList(new DefaultIndexerEventFactory());
    @Override
    public IndexerEventFactory getIndexerFactoryFor(Object o) {
        if(factories !=null){
            for(IndexerEventFactory f : factories){
                if(f.supports(o)){
                    return f;
                }
            }
        }
        return null;
    }
    
    @Override
    public IndexerEventFactory getIndexerFactoryForKey(Key k) {
        if(factories !=null){
            for(IndexerEventFactory f : factories){
                if(f.supportsKey(k)){
                    return f;
                }
            }
        }
        return null;
    }
}
