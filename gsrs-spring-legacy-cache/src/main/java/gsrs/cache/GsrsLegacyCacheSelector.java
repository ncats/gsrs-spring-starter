package gsrs.cache;

import ix.ncats.controllers.auth.LegacyUserTokenCache;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class GsrsLegacyCacheSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{
                GsrsLegacyCachePropertyConfiguration.class.getName(),
                GsrsLegacyCacheConfiguration.class.getName(),
                LegacyUserTokenCache.class.getName()};
    }
}
