package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name="ix_core_filedata")
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("FIG")
@DiscriminatorOptions(force = true)
public class FileData extends BaseModel {
    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false)
    public UUID id; // internal id
    public String mimeType;
    
    
    @Lob
    @JsonIgnore
    @Indexable(indexed=false)    
    @Basic(fetch= FetchType.EAGER)
    public byte[] data;

    @Column(name="data_size")
    public long size;
    @Column(length=140)
    public String sha1;

    public FileData() {}

	@Override
	public String fetchGlobalId() {
		if(this.id==null)return null;
		return id.toString();
	}
    
}
