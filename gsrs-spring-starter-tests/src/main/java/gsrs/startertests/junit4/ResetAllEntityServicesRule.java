package gsrs.startertests.junit4;

import gsrs.service.GsrsEntityService;
import org.junit.rules.ExternalResource;

public class ResetAllEntityServicesRule extends ExternalResource {
    @Override
    protected void before(){
        GsrsEntityService.ENTITY_SERVICE_INTIALIZATION_GROUP.resetCache();
    }
}
