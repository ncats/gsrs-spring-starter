package gsrs.sequence.indexer;

import gsrs.indexer.IndexCreateEntityEvent;
import ix.core.models.SequenceEntity;
import ix.core.util.EntityUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SequenceEntityIndexCreateEvent extends IndexCreateEntityEvent {
    SequenceEntity.SequenceType sequenceType;

    public SequenceEntityIndexCreateEvent(EntityUtils.Key key, SequenceEntity.SequenceType sequenceType) {
        super(key);
        this.sequenceType = sequenceType;
    }
}
