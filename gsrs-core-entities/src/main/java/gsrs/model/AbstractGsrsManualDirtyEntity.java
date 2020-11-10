package gsrs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gsrs.GsrsEntityProcessorListener;
import gsrs.GsrsManualDirtyMaker;
import ix.core.search.text.TextIndexerEntityListener;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * An {@link AbstractGsrsEntity} that allows for
 * programmatic setting of dirty fields.
 */
@MappedSuperclass
public abstract class AbstractGsrsManualDirtyEntity extends AbstractGsrsEntity implements GsrsManualDirtyMaker{

    @JsonIgnore
    @Transient
    private Set<String> dirtyFields = new HashSet<>();

    @Override
    public Set<String> getDirtyFields() {
        return dirtyFields;
    }
    @Override
    public void setIsDirty(String field) {
        this.dirtyFields.add(Objects.requireNonNull(field));
    }

    @Override
    public void clearDirtyFields() {
        dirtyFields.clear();
    }
}
