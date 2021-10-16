package gsrs;

import ix.core.models.ForceUpdatableModel;

public interface ForceUpdateDirtyMakerMixin extends GsrsManualDirtyMakerMixin, ForceUpdatableModel{

    @Override
    default void forceUpdate() {
        setIsAllDirty();
    }
    
}
