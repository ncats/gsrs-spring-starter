package gsrs.startertests.junit4;

import gov.nih.ncats.common.util.CachedSupplier;
import org.junit.rules.ExternalResource;

public class ResetAllCacheSuppliersRule extends ExternalResource {

    @Override
    protected void before(){
        CachedSupplier.resetAllCaches();
    }
}
