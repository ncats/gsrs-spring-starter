package gsrs.sequence.indexer;

import java.util.Optional;

import gsrs.indexer.IndexUpdateEntityEvent;
import ix.core.models.SequenceEntity;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SequenceEntityUpdateCreateEvent extends IndexUpdateEntityEvent {
    SequenceEntity.SequenceType sequenceType;

    public SequenceEntityUpdateCreateEvent(EntityUtils.Key key,Optional<EntityWrapper<?>> optional,  SequenceEntity.SequenceType sequenceType) {
        super(key,optional);
        this.sequenceType = sequenceType;
    }
    
    public SequenceEntityUpdateCreateEvent(EntityUtils.Key key, SequenceEntity.SequenceType sequenceType) {
        this(key,Optional.empty(), sequenceType);
    }
}
