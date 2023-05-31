package gsrs.stagingarea.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import ix.core.EntityMapperOptions;
import ix.core.models.*;
import ix.ginas.converters.GinasAccessConverter;
import ix.ginas.models.GinasAccessContainer;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.serialization.GsrsDateDeserializer;
import ix.ginas.models.serialization.PrincipalDeserializer;
import ix.ginas.models.serialization.PrincipalSerializer;
import ix.ginas.models.utils.JSONEntity;
import ix.ginas.models.serialization.GsrsDateDeserializer;
import ix.ginas.models.serialization.GsrsDateSerializer;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

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

    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    private UUID instanceId; //always unique!  changes when data change

    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    //@OneToOne
    private UUID recordId; //stays the same for a given record

    @Indexable(facet = true)
    private int version;

    @Indexable(name = "SourceName", suggest = true)
    private String sourceName;

    @CreatedDate
    @Indexable(facet=true, sortable = true, name = "Load Date")
    @JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    private Date versionCreationDate =null;

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
    @Column(length = 255)
    private String entityClassName;

    /*
    To record why this import was rejected or processed in a certain way.
     */
    @Indexable()
    private String reason;

    @JSONEntity(title = "KeyValueMappings")
    @OneToMany()
    @JoinColumns({
            @JoinColumn(name="instanceId", referencedColumnName = "instance_id")
    })
    @JsonView(BeanViews.Full.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    @ToString.Exclude
    @ElementCollection(fetch = FetchType.EAGER) //testing out eager fetch 05 May 2023
    public List<KeyValueMapping> keyValueMappings = new ArrayList<>();

    @JSONEntity(title = "ImportValidations")
    @JsonView(BeanViews.Full.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    @OneToMany
    @JoinColumns({
            @JoinColumn(name="instanceId", referencedColumnName = "instance_id")
    })
    @ToString.Exclude
    @ElementCollection(fetch = FetchType.EAGER)
    public List<ImportValidation> validations = new ArrayList<>();

    @Indexable
    private String dataFormat;

    @JsonIgnore
    public GinasAccessContainer getRecordAccess() {
        return recordAccess;
    }

    @Indexable
    private String importAdapter;

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