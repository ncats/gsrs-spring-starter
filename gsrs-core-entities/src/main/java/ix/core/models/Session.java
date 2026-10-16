package ix.core.models;

import gov.nih.ncats.common.util.TimeUtil;
import ix.core.History;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="ix_core_session")
@Indexable(indexed=false)
@History(store=false)
public class Session extends BaseModel {
    @Id
    @GenericGenerator(name = "NullUUIDGenerator", type = ix.ginas.models.generators.NullUUIDGenerator.class)
    @GeneratedValue(generator = "NullUUIDGenerator")
    public UUID id;

    @ManyToOne(cascade=CascadeType.ALL)
    public UserProfile profile;
    
    public final long created = TimeUtil.getCurrentTimeMillis();
    public long accessed = TimeUtil.getCurrentTimeMillis();
    public String location;
    public boolean expired;
        
    public Session() {}
    public Session(UserProfile profile) {
        this.profile = profile;
    }
	@Override
	public String fetchGlobalId() {
		if(this.id==null)return null;
		return this.id.toString();
	}
}
