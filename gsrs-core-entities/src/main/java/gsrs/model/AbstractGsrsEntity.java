package gsrs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import gsrs.BackupEntityProcessorListener;
import gsrs.GSRSEntityTraits;
import gsrs.GsrsEntityProcessorListener;
import gsrs.ParentAware;
import gsrs.indexer.IndexerEntityListener;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base abstract class for Gsrs Entities should extend,
 * includes support for {@link ix.core.EntityProcessor}s,
 * auditing, and TextIndexing.
 */
@MappedSuperclass
//hibernate proxies add some extra fields we want to ignore during json serialization
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EntityListeners(value= {AuditingEntityListener.class,
                        BackupEntityProcessorListener.class,
                        GsrsEntityProcessorListener.class,
                        IndexerEntityListener.class})
public abstract class AbstractGsrsEntity implements GSRSEntityTraits, ParentAware{

}
