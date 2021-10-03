package gsrs;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface GsrsManualDirtyMaker {

    void setIsDirty(String dirtyField);
    Set<String> getDirtyFields();
    void clearDirtyFields();
    boolean isDirty(String field);

    /**
     * Only invoke the given action if the field is not yet dirty.
     * @param field
     * @param action the action to invoke; can not be null.
     * @throws NullPointerException if any parameter is null.
     */
    void performIfNotDirty(String field, Runnable action);


    @JsonIgnore
    default boolean isDirty() {
        return !this.getDirtyFields().isEmpty();
    }
}
