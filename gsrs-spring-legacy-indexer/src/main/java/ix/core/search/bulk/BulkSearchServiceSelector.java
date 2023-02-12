package ix.core.search.bulk;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class BulkSearchServiceSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{
        		BulkSearchService.class.getName()
        		};
    }
}
