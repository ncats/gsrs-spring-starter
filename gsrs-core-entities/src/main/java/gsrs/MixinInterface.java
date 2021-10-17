package gsrs;

import java.util.Map;
import java.util.function.Function;

public interface MixinInterface {

    default Map<String,Object> _getMixinStore(){
        return MixinUtil.getStore(this);
    }
    
    default void _clearMixinStore(){
        MixinUtil.clearStore(this);
    }
    
    default <T> void _setField(String name, T v) {
        MixinUtil.getStore(this).put(name, v);
    }
    
    @SuppressWarnings("unchecked")
    default <T> T _getFieldOrDefault(String name, T def) {
        return (T) MixinUtil.getStore(this).getOrDefault(name, def);
    }

    @SuppressWarnings("unchecked")
    default <T> T _computeFieldIfAbsent(String name, Function<String,T> vmaker) {
        return (T) MixinUtil.getStore(this).computeIfAbsent(name,vmaker);
    }
}
