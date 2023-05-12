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

    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = false)
    @Indexable(name="RecordId")
    private UUID recordId;

    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    @Indexable(name="instanceId")
    private UUID instanceId;

    @Indexable(name="Version")
    private int version;

    @Lob
    private String data;

    @Indexable
    @Column(length = 255)
    private String entityClassName;

    @Indexable
    private Date saveDate;
}
