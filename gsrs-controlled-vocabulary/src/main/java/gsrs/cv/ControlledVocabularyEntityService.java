package gsrs.cv;

import gsrs.service.GsrsEntityService;
import ix.ginas.models.v1.ControlledVocabulary;

import java.util.Optional;

public interface ControlledVocabularyEntityService extends GsrsEntityService<ControlledVocabulary, Long> {
    Optional<ControlledVocabulary> get(Long id);
}
