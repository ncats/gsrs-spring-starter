package ix.core.models;

import org.hibernate.annotations.GenericGenerator;

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

//    @GeneratedValue //Ebean added GeneratedValue by default we have to be explicit in hibernate
//	@Id
//	@GeneratedValue(strategy= GenerationType.SEQUENCE, generator = "non-nullGen")
//	@GenericGenerator(name = "non-nullGen", strategy = "ix.ginas.models.generators.NullLongGenerator")
	public Long id;
	
	@Override
	public String fetchGlobalId() {
		if(id!=null)return this.getClass().getName() + ":" + id.toString();
		return null;
	}
	
}
