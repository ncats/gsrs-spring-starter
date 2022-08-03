package gsrs.holdingarea.model;

import ix.core.models.Indexable;
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

@Entity
@Table(name = "ix_import_validation", indexes = {@Index(name="idx_ix_import_validation_entity_class_name", columnList = "entityClassName"),
        @Index(name="idx_ix_import_validation_version", columnList = "version"),
        @Index(name="idx_ix_import_validation_instance_id", columnList = "instanceId")})
/*,
        @Index(name="idx_ix_import_validation_type", columnList = "ValidationType")*/
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportValidation {

    public enum ImportValidationType {
        info,
        warning,
        error
    }


    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = false)
    @Indexable
    private UUID instanceId;

    @Indexable
    private int version;

    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    @Indexable
    private UUID ValidationId;

    @Indexable
    @Column(length =255, updatable = false, unique = false)
    private ImportValidationType ValidationType;

    @Indexable
    private String ValidationMessage;

    @Lob
    @Indexable
    private String ValidationJson;

    @Indexable
    private Date ValidationDate;

    @Indexable
    @Column(length = 255, name="entityClassName")
    private String entityClassName;
}
