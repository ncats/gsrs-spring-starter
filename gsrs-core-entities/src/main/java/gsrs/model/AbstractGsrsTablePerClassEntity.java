package gsrs.model;

import javax.persistence.EntityListeners;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import gsrs.BackupEntityProcessorListener;
import gsrs.GSRSEntityTraits;
import gsrs.GsrsEntityProcessorListener;
import gsrs.indexer.IndexerEntityListener;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base abstract class for Gsrs Entities should extend,
 * includes support for {@link ix.core.EntityProcessor}s,
 * auditing, and TextIndexing.
 */
//@MappedSuperclass
//hibernate proxies add some extra fields we want to ignore during json serialization
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EntityListeners(value= {AuditingEntityListener.class, GsrsEntityProcessorListener.class, IndexerEntityListener.class, BackupEntityProcessorListener.class})
public abstract class AbstractGsrsTablePerClassEntity implements GSRSEntityTraits{


}
