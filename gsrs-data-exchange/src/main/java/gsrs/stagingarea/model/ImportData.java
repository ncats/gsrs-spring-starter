package gsrs.stagingarea.model;

import ix.core.models.Backup;
import ix.core.models.Indexable;
import ix.core.models.IndexableRoot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;

import java.util.Date;
import java.util.UUID;

/**
 * Representation of one record (domain entity) imported from a file.
 * Each time the record is changed, a new record in this table is created.
 * All records for the same entity will have the same record_id.
 * Record_id is a foreign key to the ImportMetadata object
 */
@Backup
@Table(name = "ix_import_data", indexes={@Index(name="idx_ix_import_data_entity_class_name", columnList = "entity_class_name"),
        @Index(name="idx_ix_import_data_version", columnList = "version"), @Index(name="idx_ix_import_data_record_id", columnList = "record_id")})
@Slf4j
@Data
@Entity
@IndexableRoot
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ImportData {

    /**
     * Foreign key, referencing ImportMetadata
     */
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = false)
    @Indexable(name="RecordId")
    private UUID recordId;

    /**
     * Primary key
     */
    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true, name = "instance_id")
    @Indexable(name="instanceId")
    private UUID instanceId;

    /**
     * sequential integer to track revisions to this record.
     * First instance of a given record's version is set to 1 when the record is created.
     * After each change, a new ImportData instance is created with a version incremented by 1
     */

    @Indexable(name="Version")
    private int version;

    /**
     * Domain entity as a JSON CLOB
     */
    @Lob
    private String data;

    /**
     * qualified name of the class of domain object being imported
     */
    @Indexable
    @Column(length = 255)
    private String entityClassName;

    /**
     * Date on which this version was saved to the (staging area) database
     */
    @Indexable
    private Date saveDate;
}
