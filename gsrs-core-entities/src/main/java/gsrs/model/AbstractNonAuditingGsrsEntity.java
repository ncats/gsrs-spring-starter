package gsrs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import gsrs.BackupEntityProcessorListener;
import gsrs.GsrsEntityProcessorListener;
import gsrs.indexer.IndexerEntityListener;
import ix.core.util.EntityUtils;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;

/**
 * Base abstract class for Gsrs Entities should extend,
 * includes support for {@link ix.core.EntityProcessor}s,
 * auditing, and TextIndexing.
 */
@MappedSuperclass
//hibernate proxies add some extra fields we want to ignore during json serialization
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EntityListeners(value= {GsrsEntityProcessorListener.class, IndexerEntityListener.class, BackupEntityProcessorListener.class})
public abstract class AbstractNonAuditingGsrsEntity {

    //dkatzel don't use these anymore should be performance improvement to not do it?

//    @JsonIgnore
//    @Transient
//    private JsonNode previousState;
//    @JsonIgnore
//    @Transient
//    private String previousVersion;
//
//    @PostLoad
//    public void updatePreviousState(){
//        EntityUtils.EntityWrapper<AbstractNonAuditingGsrsEntity> ew = EntityUtils.EntityWrapper.of(this);
//
//        this.previousState = ew.toFullJsonNode();
//        this.previousVersion = ew.getVersion().orElse(null);
//    }
//
//    public JsonNode getPreviousState(){
//        return previousState;
//    }
//
//    public String getPreviousVersion(){
//        return previousVersion;
//    }

}
