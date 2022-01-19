package gsrs.sequence.search.legacy;


import gsrs.sequence.indexer.SubunitIndexerEventFactory;
import ix.seqaln.SequenceIndexerEventListener;
import ix.seqaln.configuration.LegacySequenceAlignmentConfiguration;
import ix.seqaln.service.LegacySequenceIndexerService;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class GsrsLegacySequenceIndexerSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{
                SubunitIndexerEventFactory.class.getName(),
                LegacySequenceAlignmentConfiguration.class.getName(),
                LegacySequenceIndexerService.class.getName(),
                SequenceIndexerEventListener.class.getName()};
    }
}
