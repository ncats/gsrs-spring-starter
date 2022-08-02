package gsrs.holdingArea.model;

import com.fasterxml.jackson.annotation.JsonView;
import ix.core.EntityMapperOptions;
import ix.core.models.Backup;
import ix.core.models.BeanViews;
import ix.core.models.Indexable;
import ix.core.models.IndexableRoot;
import ix.ginas.models.utils.JSONEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Backup
@Entity
@Table(name = "ix_import_metadata", indexes = {@Index(name="idx_ix_import_metadata_entity_class_name", columnList = "entityClassName")})
@Slf4j
@IndexableRoot
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportMetadata implements Serializable {

    public enum RecordImportStatus {
        staged, //first status
        accepted, //someone reviewed and approved it
        imported, //imported as new record in permanent db
        merged, //data from this record was copied into an existing record
        rejected  //someone decided not to use this record
    }

    public enum RecordImportType {
        unknown,
        create,
        merge
    }

    public enum RecordVersionStatus {
        current,
        superseded
    }

    public enum RecordValidationStatus {
        pending,
        valid,
        warning,
        error,
        unparseable
    }

    public enum RecordProcessStatus {
        received, //we've created rows for the record in our table but haven't processed yet
        parsed, //done basic parsing but haven't fully loaded
        loaded,
        validated,
        indexed
    }

    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true, name = "instanceId")
    private UUID instanceId; //always unique!  changes when data change

    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true, name = "groupId")
    //@OneToOne
    private UUID recordId; //stays the same for a given record

    @Indexable(facet = true)
    private int version;

    @Indexable(name = "SourceName", suggest = true)
    private String sourceName;

    @Indexable
    private Date versionCreationDate;

    @Indexable(name="ImportStatus", facet = true)
    private RecordImportStatus importStatus;

    @Indexable
    private RecordImportType importType;

    @Indexable(facet = true)
    private RecordVersionStatus versionStatus;

    @Indexable
    private RecordValidationStatus validationStatus;

    @Indexable(name="processStatus", facet = true)
    private RecordProcessStatus processStatus;

    @Indexable
    @Column(length = 255, name="entityClassName")
    private String entityClassName;

    /*
    To record why this import was rejected or processed in a certain way.
     */
    @Indexable()
    private String reason;

    @JSONEntity(title = "KeyValueMappings")
    @OneToMany()
    @JoinColumns({
            @JoinColumn(name="instanceId", referencedColumnName = "instanceId")
    })
    @JsonView(BeanViews.Full.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    public List<KeyValueMapping> KeyValueMappings = new ArrayList<>();

    @JSONEntity(title = "ImportValidations")
    @JsonView(BeanViews.Full.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    @OneToMany
    @JoinColumns({
            @JoinColumn(name="instanceId", referencedColumnName = "instanceId")
    })
    public List<gsrs.holdingArea.model.ImportValidation> validations = new ArrayList<>();

    @Indexable
    private String dataFormat;

}