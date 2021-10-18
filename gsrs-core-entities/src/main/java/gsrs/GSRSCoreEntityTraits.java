package gsrs;

import ix.core.util.EntityUtils.EntityWrapper;

public interface GSRSCoreEntityTraits {
    
    default <T> EntityWrapper<T> getEntityWrapper() {
        return (EntityWrapper<T>) EntityWrapper.of(this);
    }

}
