package gsrs;

import ix.core.EntityProcessor;

public interface EntityProcessorFactory {
    EntityProcessor getCombinedEntityProcessorFor(Object o);
}
