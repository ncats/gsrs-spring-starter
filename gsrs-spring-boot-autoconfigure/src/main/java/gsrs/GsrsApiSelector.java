package gsrs;

import gsrs.controller.GsrsWebConfig;
import gsrs.indexer.IndexValueMakerFactory;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

public class GsrsApiSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(EnableGsrsApi.class.getName(), false));
        EnableGsrsApi.IndexerType indexerType = attributes.getEnum("indexerType");
        switch(indexerType){
            case LEGACY: return new String[]{GsrsWebConfig.class.getName(), IndexValueMakerFactory.class.getName()};
        }
        return new String[]{GsrsWebConfig.class.getName()};
    }
}
