package gsrs.cv.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GsrsFragmentControlledVocabularyDTO extends AbstractGsrsControlledVocabularyDTO{

    private List<FragmentTermDTO> terms;
}
