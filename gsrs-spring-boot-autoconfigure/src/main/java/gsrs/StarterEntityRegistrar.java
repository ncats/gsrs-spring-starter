package gsrs;

import gsrs.repository.PrincipalRepository;
import ix.core.models.IxModel;
import ix.ginas.models.GinasCommonData;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class StarterEntityRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        AutoConfigurationPackages.register(registry, "ix");
        AutoConfigurationPackages.register(registry, "gsrs");
        //gov.nih.ncats
        AutoConfigurationPackages.register(registry, "gov.nih.ncats");
        AutoConfigurationPackages.register(registry, PrincipalRepository.class.getPackage().getName());

    }
}
