package gsrs.holdingarea.model;

import ix.core.models.Indexable;
import ix.core.models.IndexableRoot;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;
import ix.core.models.IndexableRoot;

@Entity
@Table(name = "ix_import_mapping")
@Slf4j
@IndexableRoot
@Data
public class KeyValueMapping {

    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    private UUID recordId;

    @Indexable(name = "Key", suggest = true)
    private String key;

    @Indexable(name = "Value", suggest = true)
    private String value;

    @Indexable(name = "Qualifier", suggest = true)
    private String qualifier;
}
