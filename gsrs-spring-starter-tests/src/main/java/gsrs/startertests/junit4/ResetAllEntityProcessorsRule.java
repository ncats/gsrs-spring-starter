package gsrs.startertests.junit4;

import gsrs.EntityProcessorFactory;
import org.junit.rules.ExternalResource;

public class ResetAllEntityProcessorsRule extends ExternalResource {
    @Override
    protected void before() throws Throwable {
        EntityProcessorFactory.ENTITY_PROCESSOR_FACTORY_INITIALIZER_GROUP.resetCache();
    }
}
