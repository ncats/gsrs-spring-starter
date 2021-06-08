package gsrs.cv;

import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.repository.ControlledVocabularyRepository;
import gsrs.repository.GsrsRepository;
import ix.ginas.models.v1.ControlledVocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CvLegacySearchService extends LegacyGsrsSearchService<ControlledVocabulary> {
    @Autowired
    public CvLegacySearchService(ControlledVocabularyRepository repository) {
        super(ControlledVocabulary.class, repository);
    }

}
