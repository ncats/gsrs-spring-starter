package gsrs.autoconfigure;

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
})
public class GsrsApiAutoConfiguration {



}
