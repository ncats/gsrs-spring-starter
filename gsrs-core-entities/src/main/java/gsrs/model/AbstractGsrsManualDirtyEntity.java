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
    
    @JsonIgnore
    @Transient
    private transient boolean allDirty=false;

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
        allDirty=false;
    }

    @Override
    public boolean isAllDirty() {
        return allDirty;
    }
    @Override
    public void setIsAllDirty() {
        this.allDirty=true;
    }
}
