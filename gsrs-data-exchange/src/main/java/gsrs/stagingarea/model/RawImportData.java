package gsrs.stagingarea.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.models.ParentReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "ix_import_raw")
@Slf4j
@Data
public class RawImportData {

    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    private UUID id;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JsonIgnore
    @ParentReference
    private ImportMetadata owner;

    @Lob
    private byte[] rawData;

    private String recordFormat;
}
