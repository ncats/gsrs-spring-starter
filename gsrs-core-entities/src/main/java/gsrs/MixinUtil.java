package gsrs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ix.utils.LiteralReference;

public class MixinUtil {

    private static Map<LiteralReference<Object>,Map<String,Object>> _store = new ConcurrentHashMap<>();
    
    protected static Map<String,Object> getStore(Object o) {
        return _store.computeIfAbsent(LiteralReference.of(o), k->{
            return new HashMap<String,Object>();
        });
    }
    
    protected static void clearStore(Object o) {
        _store.remove(LiteralReference.of(o));
    }
}
