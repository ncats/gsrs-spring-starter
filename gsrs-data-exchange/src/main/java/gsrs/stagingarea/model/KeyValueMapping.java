package gsrs.stagingarea.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ix.core.models.Indexable;
import ix.core.models.IndexableRoot;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Map;
import java.util.UUID;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "ix_import_mapping", indexes = {@Index(name="idx_ix_import_mapping_key", columnList = "Mapping_Key"),
        @Index(name="idx_ix_import_mapping_value", columnList = "mapping_value"),
        @Index(name="idx_ix_import_mapping_instance_id", columnList = "instance_Id")})
@Slf4j
@IndexableRoot
@Data
public class KeyValueMapping {

    public static final int MAX_VALUE_LENGTH = 512;

    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    private UUID mappingId;

    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = false)
    private UUID instanceId;

    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = false)
    private UUID recordId;

    @Indexable(name = "Mapping_Key", suggest = true)
    @Column(name = "mapping_key")
    private String key;

    @Indexable(name = "Mapping_Value", suggest = true)
    @Column(length = MAX_VALUE_LENGTH, name = "mapping_value")
    private String value;

    @Indexable(name = "Qualifier", suggest = true)
    private String qualifier;

    @Indexable(name="Location")
    private String dataLocation;//staging area or permanent database

    @Indexable
    private String entityClass;

    @PrePersist
    @PreUpdate
    public void tidy() {
        log.trace("tidy called");
        value=value!=null && value.length()>0 ? value.substring(0, Math.min(value.length(), MAX_VALUE_LENGTH-1)) :"";
    }
}