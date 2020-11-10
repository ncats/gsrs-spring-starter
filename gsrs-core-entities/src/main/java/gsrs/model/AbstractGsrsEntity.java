package gsrs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gsrs.GsrsEntityProcessorListener;
import ix.core.search.text.TextIndexerEntityListener;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

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

}
