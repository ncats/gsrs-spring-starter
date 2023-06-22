package gsrs.sequence.indexer;

import java.util.Optional;

import gsrs.indexer.IndexerEventFactory;
import ix.core.models.SequenceEntity;
import ix.core.util.EntityUtils;

public class SubunitIndexerEventFactory implements IndexerEventFactory {
    @Override
    public boolean supports(Object object) {
        return object instanceof SequenceEntity;
    }

    @Override
    public Object newCreateEventFor(EntityUtils.EntityWrapper ew) {
        return new SequenceEntityIndexCreateEvent(ew.getKey(), Optional.of(ew), ((SequenceEntity) ew.getValue()).computeSequenceType());

    }

    @Override
    public Object newUpdateEventFor(EntityUtils.EntityWrapper ew) {
        return new SequenceEntityUpdateCreateEvent(ew.getKey(), Optional.of(ew), ((SequenceEntity) ew.getValue()).computeSequenceType());
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
