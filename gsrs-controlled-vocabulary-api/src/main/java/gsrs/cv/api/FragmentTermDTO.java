package gsrs.cv.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class FragmentTermDTO extends GsrsVocabularyTermDTO{
    private String fragmentStructure;
    private String simplifiedStructure;
}
