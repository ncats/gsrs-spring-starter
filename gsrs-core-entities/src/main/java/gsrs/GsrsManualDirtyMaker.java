package gsrs;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface GsrsManualDirtyMaker {

    void setIsDirty(String dirtyField);
    boolean isDirty(String field);
    
    @JsonIgnore
    Set<String> getDirtyFields();
    
    void clearDirtyFields();

    /**
     * Only invoke the given action if the field is not yet dirty.
     * @param field
     * @param action the action to invoke; can not be null.
     * @throws NullPointerException if any parameter is null.
     */
    public default void performIfNotDirty(String field, Runnable action) {
        Objects.requireNonNull(action);
        if(!this.isDirty(field)) {
            action.run();    
            this.setIsDirty(field);
        }
    }


    @JsonIgnore
    default boolean isDirty() {
        return isAllDirty() || !this.getDirtyFields().isEmpty();
    }
    
  
    
    @JsonIgnore
    boolean isAllDirty();
    
    @JsonIgnore
    void setIsAllDirty();
}
