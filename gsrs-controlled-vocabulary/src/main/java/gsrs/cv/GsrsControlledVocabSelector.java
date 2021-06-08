package gsrs.cv;

import gsrs.repository.ControlledVocabularyRepository;
import gsrs.vocab.DuplicateDomainValidator;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class GsrsControlledVocabSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{
                ControlledVocabConfiguration.class.getName(),
                CvController.class.getName(),
                CvLegacySearchService.class.getName(),
                ControlledVocabularyEntityServiceImpl.class.getName(),

                DuplicateDomainValidator.class.getName(),

        };
    }
}
