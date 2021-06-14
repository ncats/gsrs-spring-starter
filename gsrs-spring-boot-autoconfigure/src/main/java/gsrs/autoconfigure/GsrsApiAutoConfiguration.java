package gsrs.autoconfigure;

import gsrs.JsonTypeIdResolverConfiguration;
import gsrs.RegisteredFunctionProperties;

import gsrs.buildInfo.BuildInfoConfiguration;
import gsrs.buildInfo.VersionFileBuildInfoFetcherConfiguation;
import gsrs.controller.GsrsApiControllerAdvice;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.service.DefaultExportService;
import gsrs.springUtils.AutowireHelper;
import gsrs.validator.ConfigBasedGsrsValidatorFactory;
import gsrs.GsrsFactoryConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
//can't do component scan in autoconfiguration so manually import our components
@Import(value = {AutowireHelper.class, GsrsControllerConfiguration.class,
        GsrsApiControllerAdvice.class,
         GsrsFactoryConfiguration.class, ConfigBasedGsrsValidatorFactory.class,
        JsonTypeIdResolverConfiguration.class, RegisteredFunctionProperties.class,
        GsrsExportConfiguration.class, DefaultExportService.class,
        BuildInfoConfiguration.class, VersionFileBuildInfoFetcherConfiguation.class
        })
public class GsrsApiAutoConfiguration {



}
