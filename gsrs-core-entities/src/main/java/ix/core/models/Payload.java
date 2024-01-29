package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.model.GsrsApiAction;
import ix.core.FieldResourceReference;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="ix_core_payload")
@Indexable(indexed = false)//we don't want this indexed
public class Payload extends BaseModel {
    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false)
    public UUID id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    public Namespace namespace;
    
    
    public final Date created = TimeUtil.getCurrentDate();
    
    @Column(length=1024)
    public String name;

    @Column(length=40)
    public String sha1;
    
    @Column(length=128)
    public String mimeType; // mime type
    @Column(name="capacity")
    public Long size;

    

    @ManyToMany(cascade= CascadeType.ALL)
    @JoinTable(name="ix_core_payload_property", inverseJoinColumns = {
            @JoinColumn(name="ix_core_value_id")
    })
    public List<Value> properties = new ArrayList<Value>();

    public Payload() {
    	
    }

    @JsonIgnore
    @GsrsApiAction(value = "url", serializeUrlOnly = true)
    public FieldResourceReference getUrl() {

        return FieldResourceReference.forSelf( ()->this, "format=raw");
    }
    public Value addIfAbsent (Value prop) {
        if (prop != null) {
            if (prop.id != null) 
                for (Value p : properties) {
                    if (p.id.equals(prop.id))
                        return p;
                }
            properties.add(prop);
        }
        return prop;
    }

	@Override
	public String fetchGlobalId() {
		if(this.id==null)return null;
		return this.id.toString();
	}
	
}
