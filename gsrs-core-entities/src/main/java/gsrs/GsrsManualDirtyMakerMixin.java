package gsrs;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface GsrsManualDirtyMakerMixin extends GsrsManualDirtyMaker, MixinInterface{
    
    
    @Override
    default void setIsDirty(String dirtyField) {
       this._computeFieldIfAbsent("dirtyFields", t-> new HashSet<String>()).add(dirtyField);
    }

    @Override
    default boolean isDirty(String field) {
        return getDirtyFields().contains(field);
    }

    @Override
    default Set<String> getDirtyFields() {
        return this._getFieldOrDefault("dirtyFields", new HashSet<String>());
    }

    @Override
    default void clearDirtyFields() {
        MixinUtil.clearStore(this);
    }

    @Override
    @JsonIgnore
    default boolean isAllDirty() {
        return (boolean) this._getMixinStore().getOrDefault("isAllDirty", Boolean.FALSE);
    }
    

    default void setIsAllDirty() {
        this._setField("isAllDirty", Boolean.TRUE);
    }
    
}
