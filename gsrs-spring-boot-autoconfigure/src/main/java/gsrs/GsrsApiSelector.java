package gsrs;

import gsrs.controller.GsrsWebConfig;
import gsrs.entityProcessor.BasicEntityProcessorConfiguration;
import gsrs.entityProcessor.ConfigBasedEntityProcessorConfiguration;
import gsrs.entityProcessor.ConfigBasedEntityProcessorFactory;
import gsrs.indexer.IndexValueMakerFactory;
import ix.core.search.text.Lucene4IndexServiceFactory;
import ix.core.search.text.TextIndexerConfig;
import ix.core.search.text.TextIndexerFactory;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;

public class GsrsApiSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(EnableGsrsApi.class.getName(), false));
        EnableGsrsApi.IndexerType indexerType = attributes.getEnum("indexerType");

        List<Class> componentsToInclude = new ArrayList<>();
        componentsToInclude.add(GsrsWebConfig.class);
        switch(indexerType){
            case LEGACY: {
                componentsToInclude.add(TextIndexerFactory.class);
                componentsToInclude.add(TextIndexerConfig.class);
                componentsToInclude.add(IndexValueMakerFactory.class);
                componentsToInclude.add(Lucene4IndexServiceFactory.class);

            }
        }
        EnableGsrsApi.EntityProcessorDetector entityProcessorDetector = attributes.getEnum("entityProcessorDetector");

        switch(entityProcessorDetector){
            case COMPONENT_SCAN:
                componentsToInclude.add(BasicEntityProcessorConfiguration.class);
                break;
            case CONF:
                componentsToInclude.add(ConfigBasedEntityProcessorConfiguration.class);
                break;
        }
        return componentsToInclude.stream().map(Class::getName)
                .peek(c-> System.out.println(c)).toArray(i-> new String[i]);
    }
}
