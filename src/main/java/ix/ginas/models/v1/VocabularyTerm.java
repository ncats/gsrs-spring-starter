package ix.ginas.models.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ix.core.SingleParent;
import ix.core.models.IxModel;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.serialization.KeywordDeserializer;
import ix.ginas.models.serialization.KeywordListSerializer;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * Created by sheilstk on 6/29/15.
 */

@Entity
@Table(name="ix_ginas_vocabulary_term")
@Inheritance
@DiscriminatorValue("VOCAB")
@SingleParent
@Getter
@Setter
public class VocabularyTerm extends IxModel {
	/**
	 * 
	 */
	@ManyToOne(cascade = CascadeType.PERSIST)
    @JsonIgnore //ignore in json to avoid infinite recursion
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
	@Column(length=4000)
	public String value;

	@Column(length=4000)
	public String display;

	@Column(length=4000)
	public String description;

	@Column(length=4000)
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


	@PreUpdate
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

}
