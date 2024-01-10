package gsrs.serviceutilities;


import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
//. import serviceutilities.GsrsServiceUtilitiesConfiguration;

public class GsrsServiceUtilitiesSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{
//            GsrsServiceUtilitiesConfiguration.class.getName()
        };
    }
}
