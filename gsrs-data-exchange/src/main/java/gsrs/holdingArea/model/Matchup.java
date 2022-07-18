package gsrs.holdingArea.model;

import ix.core.models.Backup;
import ix.core.models.Indexable;
import ix.core.models.IndexableRoot;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;

@Backup
@Table(name = "ix_import_matchup")
@Slf4j
@Data
@IndexableRoot
public class Matchup {

    public enum RecordMatchType {
        staging,
        permanent,
        external /*not sure if we'll use this*/
    }
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    @Indexable(name="RecordId")
    private UUID recordId;

    @Indexable(name="Version")
    private int version;

    @Indexable(name="Key")
    private String key;

    @Indexable(name="Value")
    private String value;

    @Indexable(name="Qualifier")
    private String qualifier;

    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    private UUID matchedRecord;

    @Indexable(name="MatchType")
    private RecordMatchType matchType;

    @Indexable(name="MatchLevel")
    private int matchLevel;
}
