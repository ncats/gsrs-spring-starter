package ix.core.models;

import jakarta.persistence.*;

/**
 * Base class of objects in our model which
 * use a long as an Id instead of a String or UUID.
 */
@MappedSuperclass
public abstract class LongBaseModel extends BaseModel {

    @Id
	@Column(unique = true)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "LONG_SEQ_ID")
	public Long id;
	
	@Override
	public String fetchGlobalId() {
		if(id!=null)return this.getClass().getName() + ":" + id.toString();
		return null;
	}
	
}
