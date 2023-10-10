package gsrs;

import org.apache.poi.ss.formula.functions.T;

import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;

public interface GSRSEntityTraits {
    
    default <T> EntityWrapper<T> fetchEntityWrapper() {
        return (EntityWrapper<T>) EntityWrapper.of(this);
    }
    
    default Key fetchKey() {
        return fetchEntityWrapper().getKey();
    }
}
