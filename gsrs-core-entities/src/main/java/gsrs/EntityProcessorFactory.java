package gsrs;

import gov.nih.ncats.common.util.CachedSupplierGroup;
import ix.core.EntityProcessor;

public interface EntityProcessorFactory {
    static final CachedSupplierGroup ENTITY_PROCESSOR_FACTORY_INITIALIZER_GROUP = new CachedSupplierGroup();
    EntityProcessor getCombinedEntityProcessorFor(Object o);

    void initialize();
}
