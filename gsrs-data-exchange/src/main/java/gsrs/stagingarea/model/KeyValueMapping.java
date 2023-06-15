package gsrs.stagingarea.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ix.core.models.Indexable;
import ix.core.models.IndexableRoot;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;

/**
 * Tracks the Key/Value/Qualifier triplets computed for all records in the staging area and in the main database
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "ix_import_mapping", indexes = {@Index(name="idx_ix_import_mapping_key", columnList = "mapping_key"),
        @Index(name="idx_ix_import_mapping_value", columnList = "mapping_value"),
        @Index(name="idx_ix_import_mapping_instance_id", columnList = "instance_id")})
@Slf4j
@IndexableRoot
@Data
public class KeyValueMapping {

    public static final int MAX_VALUE_LENGTH = 512;

    /**
     * Primary key
     */
    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    private UUID mappingId;

    /**
     * Foreign key to ImportData
     */
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = false)
    private UUID instanceId;

    /**
     * Foreign key to ImportMetadata
     */
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = false)
    private UUID recordId;

    /**
     * Key - some factor that differentiates domain entities.
     * (Each entity in the system will compute its own factors)
     */
    @Indexable(name = "mapping_key", suggest = true)
    @Column(name = "mapping_key")
    private String key;

    /**
     * Value for the above Key
     */
    @Indexable(name = "mapping_value", suggest = true)
    @Column(length = MAX_VALUE_LENGTH, name = "mapping_value")
    private String value;

    /**
     * A qualifier sometimes used to differentiate values of the same key
     */
    @Indexable(name = "Qualifier", suggest = true)
    private String qualifier;

    /**
     * Indication of where the data lives (staging area or GSRS)
     */
    @Indexable(name="Location")
    private String dataLocation;//staging area or permanent database

    /**
     * qualified name of the class of domain object being imported
     */
    @Indexable
    private String entityClass;

    /**
     * trim values that are too long
     */
    @PrePersist
    @PreUpdate
    public void tidy() {
        value=value!=null && value.length()>0 ? value.substring(0, Math.min(value.length(), MAX_VALUE_LENGTH-1)) :"";
    }
}