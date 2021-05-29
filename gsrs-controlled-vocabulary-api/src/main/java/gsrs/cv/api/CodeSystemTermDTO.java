package gsrs.cv.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CodeSystemTermDTO extends GsrsVocabularyTermDTO{

    private String systemCategory;
    private String regex;
}
