package gsrs.sequence.search.legacy;


import ix.seqaln.configuration.LegacySequenceAlignmentConfiguration;
import ix.seqaln.service.LegacySequenceIndexerService;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class GsrsLegacySequenceIndexerSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{LegacySequenceAlignmentConfiguration.class.getName(),
        LegacySequenceIndexerService.class.getName()};
    }
}
