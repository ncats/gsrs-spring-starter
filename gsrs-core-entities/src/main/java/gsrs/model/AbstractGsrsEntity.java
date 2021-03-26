package gsrs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import gsrs.GsrsEntityProcessorListener;
import ix.core.search.text.TextIndexerEntityListener;
import ix.core.util.EntityUtils;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.Transient;
import java.util.Optional;

/**
 * Base abstract class for Gsrs Entities should extend,
 * includes support for {@link ix.core.EntityProcessor}s,
 * auditing, and TextIndexing.
 */
@MappedSuperclass
//hibernate proxies add some extra fields we want to ignore during json serialization
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EntityListeners(value= {AuditingEntityListener.class, GsrsEntityProcessorListener.class, TextIndexerEntityListener.class})
public abstract class AbstractGsrsEntity{

    @JsonIgnore
    @Transient
    private JsonNode previousState;
    @JsonIgnore
    @Transient
    private String previousVersion;

    @PostLoad
    public void updatePreviousState(){
        EntityUtils.EntityWrapper<AbstractGsrsEntity> ew = EntityUtils.EntityWrapper.of(this);

        this.previousState = ew.toFullJsonNode();
        this.previousVersion = ew.getVersion().orElse(null);
    }

    public JsonNode getPreviousState(){
        return previousState;
    }

    public String getPreviousVersion(){
        return previousVersion;
    }

}
