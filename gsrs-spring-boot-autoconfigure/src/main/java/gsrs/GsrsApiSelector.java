package gsrs;

import gsrs.controller.*;
import gsrs.controller.hateoas.HttpLoopBackConfig;
import gsrs.controller.hateoas.LoopbackWebRequestHelper;
import gsrs.entityProcessor.BasicEntityProcessorConfiguration;
import gsrs.entityProcessor.ConfigBasedEntityProcessorConfiguration;
import gsrs.events.listeners.ReindexEventListener;
import gsrs.imports.ConfigBasedImportAdapterFactoryFactoryConfiguration;
import gsrs.indexer.ComponentScanIndexValueMakerConfiguration;
import gsrs.indexer.ConfigBasedIndexValueMakerConfiguration;
import gsrs.search.SearchResultController;
import gsrs.springUtils.StartupInitializer;
import gsrs.springUtils.StaticContextAccessor;
import gsrs.validator.ConfigBasedValidatorFactoryConfiguration;
import gsrs.validator.ValidatorConfigConverter;
import ix.core.initializers.GsrsInitializerPropertiesConfiguration;
import ix.core.search.text.Lucene4IndexServiceFactory;
import ix.core.search.text.TextIndexerConfig;
import ix.core.search.text.TextIndexerEntityListener;
import ix.core.search.text.TextIndexerSingletonConfiguration;
import ix.core.util.pojopointer.LambdaParseRegistry;
import ix.core.util.pojopointer.URIPojoPointerParser;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GsrsApiSelector implements ImportSelector {
    
    private void registerDataSource(Class dataSourceConfig) {
        EnableJpaRepositories ann = (EnableJpaRepositories) dataSourceConfig.getAnnotation(EnableJpaRepositories.class);
        String ref=ann.entityManagerFactoryRef();
        String[] packages= ann.basePackages();
        
        DataSourceConfigRegistry.register(ref, packages);
        
    }
    
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(EnableGsrsApi.class.getName(), false));
        EnableGsrsApi.IndexerType indexerType = attributes.getEnum("indexerType");

        List<Class> componentsToInclude = new ArrayList<>();

        Class defdata = attributes.getClass("defaultDatabaseSourceConfig");
        registerDataSource(defdata);
        
        componentsToInclude.add(defdata);

        for(Class c : attributes.getClassArray("additionalDatabaseSourceConfigs")){
            registerDataSource(c);
            
            componentsToInclude.add(c);
        }

        componentsToInclude.add(GsrsWebConfig.class);
        componentsToInclude.add(StaticContextAccessor.class);
        componentsToInclude.add(ReindexEventListener.class);
        componentsToInclude.add(BuildInfoController.class);
        componentsToInclude.add(UserController.class);
        componentsToInclude.add(HealthController.class);
        componentsToInclude.add(RelativePathController.class);
        switch(indexerType){
            case LEGACY: {
                componentsToInclude.add(SpecialFieldsProperties.class);
                componentsToInclude.add(TextIndexerSingletonConfiguration.class);
                componentsToInclude.add(TextIndexerConfig.class);
                componentsToInclude.add(TextIndexerEntityListener.class);
                componentsToInclude.add(Lucene4IndexServiceFactory.class);

            }
        }
        EnableGsrsApi.IndexValueMakerDetector indexValueMakerDetector = attributes.getEnum("indexValueMakerDetector");
        switch (indexValueMakerDetector){
            case CONF:
                componentsToInclude.add(ConfigBasedIndexValueMakerConfiguration.class);
                break;
            case COMPONENT_SCAN:
                componentsToInclude.add(ComponentScanIndexValueMakerConfiguration.class);
                break;
            default: break;
        }

        EnableGsrsApi.EntityProcessorDetector entityProcessorDetector = attributes.getEnum("entityProcessorDetector");


        switch(entityProcessorDetector){
            case COMPONENT_SCAN:
                componentsToInclude.add(BasicEntityProcessorConfiguration.class);
                break;
            case CONF:
                componentsToInclude.add(ConfigBasedEntityProcessorConfiguration.class);
                break;
            default: break;
        }

        //TODO make something other than CONF based validator?
        componentsToInclude.add(ValidatorConfigConverter.class);
        componentsToInclude.add(ConfigBasedValidatorFactoryConfiguration.class);
        componentsToInclude.add(ConfigBasedImportAdapterFactoryFactoryConfiguration.class);
        componentsToInclude.add(URIPojoPointerParser.class);
        componentsToInclude.add(LambdaParseRegistry.class);
        componentsToInclude.add(RegisteredFunctionProperties.class);
        componentsToInclude.add(ExportController.class);
        componentsToInclude.add(SearchResultController.class);
        componentsToInclude.add(LogController.class);
        componentsToInclude.add(GsrsAdminLogConfiguration.class);

        componentsToInclude.add(LoopbackWebRequestHelper.class);
        componentsToInclude.add(HttpLoopBackConfig.class);
        
        
        componentsToInclude.add(GsrsInitializerPropertiesConfiguration.class);
        componentsToInclude.add(StartupInitializer.class);
        
        return componentsToInclude.stream().map(Class::getName)
                .peek(c-> log.debug("including:" + c))
                .toArray(i-> new String[i]);
    }
}
