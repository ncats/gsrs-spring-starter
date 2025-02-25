package gsrs.cv.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class GsrsCodeSystemControlledVocabularyDTO extends AbstractGsrsControlledVocabularyDTO{
    /**
     * Vocabulary Term Type for this DTO subclass.
     */
    public static final String TERM_TYPE="ix.ginas.models.v1.CodeSystemControlledVocabulary";

    private List<CodeSystemTermDTO> terms;

    public GsrsCodeSystemControlledVocabularyDTO(){
        setVocabularyTermType(TERM_TYPE);
    }

    //here there be dragons
    //do not touch this unless you know what you are doing!
    //
    //This is a partial builder class that was based on the "delomboked" SuperBuilder lombok generated class
    //lombok will helpfully generate the rest of this class and any missing methods for us
    //but here I force the vocabularyTermType to be set to the correct value for this DTO type
    //since the vocabularyTermType will always be the same per DTO subclass and must be exactly this value
    //
    //having the builder auto-add it in the builder's constructor means users don't have to worry about setting it
    //but can override it if needed by calling the setter themselves.
    public abstract static class GsrsCodeSystemControlledVocabularyDTOBuilder<C extends GsrsCodeSystemControlledVocabularyDTO, B extends GsrsCodeSystemControlledVocabularyDTO.GsrsCodeSystemControlledVocabularyDTOBuilder<C, B>> extends AbstractGsrsControlledVocabularyDTOBuilder<C, B> {


        public GsrsCodeSystemControlledVocabularyDTOBuilder() {
            this.vocabularyTermType(TERM_TYPE);
        }
    }
}
