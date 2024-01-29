package gsrs.stagingarea.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ix.core.EntityMapperOptions;
import ix.core.models.*;
import ix.ginas.converters.GinasAccessConverter;
import ix.ginas.models.GinasAccessContainer;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.serialization.GsrsDateDeserializer;
import ix.ginas.models.serialization.PrincipalDeserializer;
import ix.ginas.models.serialization.PrincipalSerializer;
import ix.ginas.models.utils.JSONEntity;
import ix.ginas.models.serialization.GsrsDateSerializer;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * Primary entity for the staging area.
 * @since GSRS 3.1
 *
 */
@Backup
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "ix_import_metadata", indexes = {@Index(name="idx_ix_import_metadata_entity_class_name", columnList = "entity_class_name")})
@Slf4j
@IndexableRoot
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
//@NoArgsConstructor    commented out because of "constructor ImportMetadata() is already defined"
public class ImportMetadata implements Serializable, GinasAccessControlled {

    //OLD WAY
    @JsonIgnore
    @Basic(fetch = FetchType.LAZY)
    @Convert(converter = GinasAccessConverter.class)
    @ToString.Exclude
    private GinasAccessContainer recordAccess;

    @Override
    public Set<Group> getAccess() {
        GinasAccessContainer gac = getRecordAccess();
        if (gac != null) {
            return gac.getAccess();
        }
        return new LinkedHashSet<Group>();
    }

    @Override
    public void setAccess(Set<Group> access) {
        this.recordAccess = new GinasAccessContainer(this);
        if (recordAccess != null) {
            this.recordAccess.setAccess(recordAccess.getAccess());
        }
    }

    @Override
    public void addRestrictGroup(Group p) {
        GinasAccessContainer gac = this.getRecordAccess();
        if (gac == null) {
            gac = new GinasAccessContainer(this);
        }
        gac.add(p);
        this.setRecordAccess(gac);
    }

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

    @GenericGenerator(name = "NullUUIDGenerator", type = ix.ginas.models.generators.NullUUIDGenerator.class)
    @GeneratedValue(generator = "NullUUIDGenerator")
    private UUID instanceId; //always unique!  changes when data change

    /**
     * Primary key.  value is assigned in code
     */
    @Id
    @GenericGenerator(name = "NullUUIDGenerator", type = ix.ginas.models.generators.NullUUIDGenerator.class)
    @GeneratedValue(generator = "NullUUIDGenerator")
    //@OneToOne
    private UUID recordId; //stays the same for a given record

    /**
     * sequential integer to track revisions to this record.
     * Initially set to 1  when the record is created and incremented after any change
     */
    @Indexable(facet = true)
    private int version;

    /**
     * File from which this record was taken
     */
    @Indexable(name = "SourceName", suggest = true)
    private String sourceName;

    /**
     * Date on which this record was added to the staging area
     */
    @CreatedDate
    @Indexable(facet=true, sortable = true, name = "Load Date")
    @JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    @Builder.Default
    private Date versionCreationDate =null;

    /**
     * How far along the import process this record is.
     * staged - just added to the staging area
     * imported - copied into the main GSRS database
     * merged - data from selected fields has been copied into a matching record in the main database
     * rejected - the user consciously marks this record as not to be used.
     */
    @Indexable(name="ImportStatus", facet = true)
    private RecordImportStatus importStatus;

    @Indexable
    private RecordImportType importType;

    @Indexable(facet = true)
    private RecordVersionStatus versionStatus;

    /**
     * Value of RecordValidationStatus applied to this record
     * currently used values:
     *  valid - obeys all business rules
     *  warning - one or more rules flags the record for further examination.
     *  error - record has a serious error that must be addressed
     */
    @Indexable
    private RecordValidationStatus validationStatus;

    /**
     * Value of RecordProcessStatus assigned to this record.
     * Note: as of June 2023, this field is set to loaded and never updated
     */
    @Indexable(name="processStatus", facet = true)
    private RecordProcessStatus processStatus;

    /**
     * qualified name of the class of domain object being imported
     */
    @Indexable
    @Column(length = 255)
    private String entityClassName;

    /*
    To record why this import was rejected or processed in a certain way.
    Not used, as of June 2023
     */
    @Indexable()
    private String reason;

    /**
     * List of factors that suggest that this entity is similar to other entities in the main database or staging area
     */
    @JSONEntity(title = "KeyValueMappings")
    @OneToMany()
    @CollectionTable(name="ix_import_mapping",
        joinColumns = @JoinColumn(name="instanceId", referencedColumnName = "instance_id")
    )
    @JsonView(BeanViews.Full.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    @ToString.Exclude
    @ElementCollection(fetch = FetchType.EAGER) //testing out eager fetch 05 May 2023
    @Builder.Default
    public List<KeyValueMapping> keyValueMappings = new ArrayList<>();

    /**
     * Link to the results of validation (applying business rules to this object)
     */
    @JSONEntity(title = "ImportValidations")
    @JsonView(BeanViews.Full.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    @OneToMany
    @CollectionTable(name="ix_import_validation",
        joinColumns = @JoinColumn(name="instanceId", referencedColumnName = "instance_id")
    )
    @ToString.Exclude
    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    public List<ImportValidation> validations = new ArrayList<>();

    /**
     * Mime typeof raw import data.
     * Not particularly useful as of June 2023
     */
    @Indexable
    private String dataFormat;

    @JsonIgnore
    public GinasAccessContainer getRecordAccess() {
        return recordAccess;
    }

    /**
     * ImportAdapterFactory used to load this entity from file
     */
    @Indexable
    private String importAdapter;

    /**
     * User who loaded this record
     */
    @CreatedBy
    @Indexable(facet = true, name = "Loaded By", sortable = true, recurse = false)
    @ManyToOne()
    @JsonDeserialize(using = PrincipalDeserializer.class)
    @JsonSerialize(using = PrincipalSerializer.class)
    public Principal importedBy;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ImportMetadata that = (ImportMetadata) o;
        boolean  equalsValue= recordId != null && Objects.equals(recordId, that.recordId);
        if(equalsValue && this.instanceId!=null){
            equalsValue = equalsValue && Objects.equals(this.instanceId, that.instanceId);
        }
        if(equalsValue && this.dataFormat != null) {
            equalsValue = equalsValue && Objects.equals(this.dataFormat, that.dataFormat);
        }
        if(equalsValue && this.entityClassName !=null) {
            equalsValue = equalsValue && Objects.equals(this.entityClassName, that.entityClassName);
        }
        if(equalsValue && this.processStatus !=null) {
            equalsValue = equalsValue && Objects.equals(this.processStatus, that.processStatus);
        }

        return equalsValue;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}