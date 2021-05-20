package gsrs.structure.legacy;


import gsrs.legacy.structureIndexer.LegacyStructureIndexerService;
import gsrs.legacy.structureIndexer.StructureIndexerEventListener;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class GsrsLegacyStructureIndexerSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{
                LegacyStructureIndexerService.class.getName(),
                StructureIndexerEventListener.class.getName()};
    }
}
