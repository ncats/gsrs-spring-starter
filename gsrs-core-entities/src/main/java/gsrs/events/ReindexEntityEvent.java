package gsrs.events;

import java.util.Optional;
import java.util.UUID;

import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReindexEntityEvent implements ReindexEvent {

    private UUID reindexId;
    private EntityUtils.Key entityKey;
    
    public ReindexEntityEvent(UUID reindexId, EntityUtils.Key entityKey) {
        this.reindexId=reindexId;
        this.entityKey=entityKey;
    }
    
    private Optional<EntityWrapper<?>> optionalEntityWrapper = Optional.empty();
    
    
    public Optional<EntityWrapper<?>> getOptionalFetchedEntityToReindex(){
        if(optionalEntityWrapper.isPresent())return optionalEntityWrapper;
        return entityKey.fetch();
        
    }
    
}
