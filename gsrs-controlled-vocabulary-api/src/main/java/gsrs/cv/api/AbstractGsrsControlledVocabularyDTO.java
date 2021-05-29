package gsrs.cv.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "vocabularyTermType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GsrsControlledVocabularyDTO.class, name = "ix.ginas.models.v1.ControlledVocabulary"),
        @JsonSubTypes.Type(value = GsrsFragmentControlledVocabularyDTO.class, name = "ix.ginas.models.v1.FragmentControlledVocabulary"),
        @JsonSubTypes.Type(value = GsrsCodeSystemControlledVocabularyDTO.class, name = "ix.ginas.models.v1.CodeSystemControlledVocabulary")
})
public class AbstractGsrsControlledVocabularyDTO {

    private long id;
    private long version =1;
    private Date created;
    private Date modified;
    private boolean deprecated;
    private boolean editable;
    private boolean filterable;
    private String domain;

    private List<String> fields;
    private String vocabularyTermType;

}
