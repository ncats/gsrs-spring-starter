package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name="ix_core_filedata")
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("FIG")
@DiscriminatorOptions(force = true)
public class FileData extends BaseModel {
    @Id
    @GenericGenerator(name = "NullUUIDGenerator", type = ix.ginas.models.generators.NullUUIDGenerator.class)
    @GeneratedValue(generator = "NullUUIDGenerator")
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
