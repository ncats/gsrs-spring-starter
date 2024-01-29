package gsrs.model;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import gsrs.BackupEntityProcessorListener;
import gsrs.GSRSEntityTraits;
import gsrs.GsrsEntityProcessorListener;
import gsrs.indexer.IndexerEntityListener;

/**
 * Base abstract class for Gsrs Entities should extend,
 * includes support for {@link ix.core.EntityProcessor}s,
 * auditing, and TextIndexing.
 */
@MappedSuperclass
//hibernate proxies add some extra fields we want to ignore during json serialization
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EntityListeners(value= {AuditingEntityListener.class, GsrsEntityProcessorListener.class, IndexerEntityListener.class, BackupEntityProcessorListener.class})
public abstract class AbstractGsrsEntity implements GSRSEntityTraits{



}
