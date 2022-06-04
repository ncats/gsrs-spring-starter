package gsrs.holdingarea.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "ix_import_validation")
@Slf4j
@Data
public class Validation {

    public enum ImportValidationType {
        info,
        warning,
        error
    }

    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    private UUID RecordId;

    private int version;

    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    private UUID ValidationId;

    private ImportValidationType ValidationType;

    private String ValidationMessage;

    @Lob
    private String ValidationJson;

    private Date ValidationDate;
}
