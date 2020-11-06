package gsrs;

import java.util.Set;

public interface GsrsManualDirtyMaker {

    void setIsDirty(String dirtyField);
    Set<String> getDirtyFields();
    void clearDirtyFields();


}
