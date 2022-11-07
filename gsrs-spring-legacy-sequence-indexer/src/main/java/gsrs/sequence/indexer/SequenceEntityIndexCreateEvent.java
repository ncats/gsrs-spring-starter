package gsrs.sequence.indexer;

import java.util.Optional;

import gsrs.indexer.IndexCreateEntityEvent;
import ix.core.models.SequenceEntity;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SequenceEntityIndexCreateEvent extends IndexCreateEntityEvent {
    SequenceEntity.SequenceType sequenceType;

    public SequenceEntityIndexCreateEvent(EntityUtils.Key key, Optional<EntityWrapper<?>> optional, SequenceEntity.SequenceType sequenceType) {
        super(key,optional);
        this.sequenceType = sequenceType;
    }
    public SequenceEntityIndexCreateEvent(EntityUtils.Key key, SequenceEntity.SequenceType sequenceType) {
        this(key,Optional.empty(), sequenceType);
    }
}
