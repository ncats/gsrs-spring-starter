package gsrs.autoconfigure;

import gsrs.EntityProcessorFactory;
import gsrs.indexer.DefaultIndexerEventFactory;
import gsrs.indexer.DefaultIndexerEventFactoryFactory;
import gsrs.security.AdminService;
import gsrs.security.GsrsSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import gsrs.GsrsFactoryConfiguration;
import gsrs.JsonTypeIdResolverConfiguration;
import gsrs.RegisteredFunctionProperties;
import gsrs.buildInfo.BuildInfoConfiguration;
import gsrs.buildInfo.VersionFileBuildInfoFetcherConfiguation;
import gsrs.controller.GsrsApiControllerAdvice;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.controller.hateoas.HttpLoopBackConfig;
import gsrs.controller.hateoas.IxContext;
import gsrs.controller.hateoas.LoopbackWebRequestHelper;
import gsrs.service.DefaultExportService;
import gsrs.springUtils.AutowireHelper;
import gsrs.validator.ConfigBasedGsrsValidatorFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

@Configuration
//can't do component scan in autoconfiguration so manually import our components
@Import(value = {AutowireHelper.class, 
        GsrsControllerConfiguration.class,
        GsrsApiControllerAdvice.class,
        GsrsFactoryConfiguration.class, 
        ConfigBasedGsrsValidatorFactory.class,
        JsonTypeIdResolverConfiguration.class, 
        RegisteredFunctionProperties.class,
        GsrsExportConfiguration.class, 
        DefaultExportService.class,
        BuildInfoConfiguration.class, 
        VersionFileBuildInfoFetcherConfiguation.class,
        GsrsApiWebConfiguration.class,
        IxContext.class,
        LoopbackWebRequestHelper.class,
        HttpLoopBackConfig.class,
        DefaultIndexerEventFactoryFactory.class,
        DefaultIndexerEventFactory.class
})
public class GsrsApiAutoConfiguration {

    @Autowired
    private EntityProcessorFactory entityProcessorFactory;

    @Autowired
    private AdminService adminService;

    @EventListener(ApplicationReadyEvent.class)
    @Order(Integer.MAX_VALUE)
    @Transactional
    public void initializeEntityProcessors(ApplicationReadyEvent event){
        System.out.println("===\n\n\nI am here xyz\n\n\n");
        adminService.runAsAdmin(entityProcessorFactory::initialize);

    }

}
