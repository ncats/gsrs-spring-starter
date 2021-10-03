package gsrs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gsrs.GsrsManualDirtyMaker;

import javax.persistence.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link AbstractGsrsEntity} that allows for
 * programmatic setting of dirty fields.
 */
@MappedSuperclass
//@Access(value= AccessType.FIELD)
public abstract class AbstractGsrsManualDirtyEntity extends AbstractGsrsEntity implements GsrsManualDirtyMaker{

    @JsonIgnore
    @Transient
    private transient Map<String, Boolean> dirtyFields = new ConcurrentHashMap<>();

    @MatchingIgnore
    @Override
    @JsonIgnore
    @Transient
    public Set<String> getDirtyFields() {
        return dirtyFields.keySet();
    }
    @Override
    @JsonIgnore
    public boolean isDirty(String field){
        return dirtyFields.containsKey(field);
    }
    @Override
    @JsonIgnore
    @Transient
    public void setIsDirty(String field) {
        this.dirtyFields.put(Objects.requireNonNull(field), Boolean.TRUE);
    }

    @Override
    public void clearDirtyFields() {
        dirtyFields.clear();
    }

    @Override
    public void performIfNotDirty(String field, Runnable action) {
        Objects.requireNonNull(action);
        dirtyFields.computeIfAbsent(Objects.requireNonNull(field), k-> {
            action.run();
            return Boolean.TRUE;
        });
    }
}
