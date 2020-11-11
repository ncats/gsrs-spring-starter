package gsrs;

import gsrs.controller.GsrsWebConfig;
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
        return componentsToInclude.stream().map(Class::getName).toArray(i-> new String[i]);
    }
}
