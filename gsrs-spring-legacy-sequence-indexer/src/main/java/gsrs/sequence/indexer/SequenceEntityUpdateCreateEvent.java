package gsrs.sequence.indexer;

import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.indexer.IndexUpdateEntityEvent;
import ix.core.models.SequenceEntity;
import ix.core.util.EntityUtils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SequenceEntityUpdateCreateEvent extends IndexUpdateEntityEvent {
    SequenceEntity.SequenceType sequenceType;

    public SequenceEntityUpdateCreateEvent(EntityUtils.Key key, SequenceEntity.SequenceType sequenceType) {
        super(key);
        this.sequenceType = sequenceType;
    }
}
