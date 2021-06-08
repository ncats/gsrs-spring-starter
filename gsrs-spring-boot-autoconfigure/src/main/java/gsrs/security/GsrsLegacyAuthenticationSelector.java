package gsrs.security;

import gsrs.controller.LoginController;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class GsrsLegacyAuthenticationSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{
                LegacyGsrsAuthenticationProvider.class.getName(),
                LoginController.class.getName(),
//                LegacyAuthenticationFilter.class.getName(),
                LegacyGsrsSecurityConfiguration.class.getName()
//                LegacyGsrsSecurityConfiguration2.class.getName()
        };
    }
}
