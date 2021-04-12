package gsrs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gsrs.GsrsManualDirtyMaker;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * An {@link AbstractGsrsEntity} that allows for
 * programmatic setting of dirty fields.
 */
@MappedSuperclass
//@Access(value= AccessType.FIELD)
public abstract class AbstractGsrsManualDirtyEntity extends AbstractGsrsEntity implements GsrsManualDirtyMaker{

    @JsonIgnore
    @Transient
    private transient Set<String> dirtyFields = new HashSet<>();

    @MatchingIgnore
    @Override
    @JsonIgnore
    @Transient
    public Set<String> getDirtyFields() {
        return dirtyFields;
    }
    @Override
    @JsonIgnore
    @Transient
    public void setIsDirty(String field) {
        this.dirtyFields.add(Objects.requireNonNull(field));
    }

    @Override
    public void clearDirtyFields() {
        dirtyFields.clear();
    }
}
