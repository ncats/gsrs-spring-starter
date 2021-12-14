package gsrs.sequence.indexer;

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
        return new SequenceEntityIndexCreateEvent(ew.getKey(), ((SequenceEntity) ew.getValue()).computeSequenceType());

    }

    @Override
    public Object newUpdateEventFor(EntityUtils.EntityWrapper ew) {
        return new SequenceEntityUpdateCreateEvent(ew.getKey(), ((SequenceEntity) ew.getValue()).computeSequenceType());
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
