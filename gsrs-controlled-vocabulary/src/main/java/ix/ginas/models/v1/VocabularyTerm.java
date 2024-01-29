package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ix.core.SingleParent;
import ix.core.models.ForceUpdatableModel;
import ix.core.models.IxModel;
import ix.core.models.ParentReference;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.serialization.KeywordDeserializer;
import ix.ginas.models.serialization.KeywordListSerializer;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

/**
 * Created by sheilstk on 6/29/15.
 */

@Entity
@Table(name="ix_ginas_vocabulary_term", indexes = {@Index(name = "vocabulary_term_owner_index", columnList = "owner_id")})
@Inheritance
@DiscriminatorValue("VOCAB")
@SingleParent
@Getter
@Setter
@SequenceGenerator(name = "LONG_SEQ_ID", sequenceName = "ix_ginas_vocabulary_term_seq", allocationSize = 1)
public class VocabularyTerm extends IxModel implements ForceUpdatableModel{
	/**
	 * 
	 */
	@ManyToOne(cascade = CascadeType.PERSIST)
    @JsonIgnore //ignore in json to avoid infinite recursion
	@ParentReference
	private ControlledVocabulary owner;

	
	/**
	 * At some point, this was set to be ignored.
	 * 
	 * It's not clear why it was, but it's no longer ignored now 
	 * to allow for reordering on saves
	 * 
	 * @return
	 */
	//@JsonIgnore
	public Long getId(){
		return this.id;
	};

	private static final long serialVersionUID = -5625533710493695789L;
	@Column(name="term_value", length=3000)
	public String value;

	@Column(length=3000)
	public String display;

	@Column(length=4000)
	public String description;

	@Column(length=3000)
	public String regex;

	public String origin;
	//    public String filter;

	@JsonSerialize(using= KeywordListSerializer.class)
	@JsonDeserialize(contentUsing= KeywordDeserializer.DomainDeserializer.class)
	@Basic(fetch= FetchType.LAZY)
	@Convert(converter = EmbeddedKeywordList.Converter.class)
	public EmbeddedKeywordList filters = new EmbeddedKeywordList();

	public boolean hidden=false;
	public boolean selected=false;

	public VocabularyTerm(){};


	@Override
	protected void preUpdate(){
		super.preUpdate();
		updateImmutables();
	}

	public void updateImmutables(){
		this.filters= new EmbeddedKeywordList(this.filters);
	}

	@Override
	public String toString() {
		return "VocabularyTerm{" +
				"owner=" + owner +
				", value='" + value + '\'' +
				", display='" + display + '\'' +
				", description='" + description + '\'' +
				", regex='" + regex + '\'' +
				", origin='" + origin + '\'' +
				", filters=" + filters +
				", hidden=" + hidden +
				", selected=" + selected +
				"} " + super.toString();
	}


    @Override
    public void forceUpdate() {
        this.setIsAllDirty();
    }

}
