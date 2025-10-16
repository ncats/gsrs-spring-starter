package ix.ginas.models.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ix.ginas.models.serialization.CodeSystemVocabularyTermListDeserializer;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import java.util.ArrayList;
import java.util.List;


@Entity
@Inheritance
@DiscriminatorValue("CSCV")
public class CodeSystemControlledVocabulary extends ControlledVocabulary {

	@Override
	@JsonDeserialize(using = CodeSystemVocabularyTermListDeserializer.class)
	public void setTerms(List<VocabularyTerm> terms) {
		this.terms = new ArrayList<VocabularyTerm>(terms);
	}


}
