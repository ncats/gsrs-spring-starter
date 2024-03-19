package gsrs.controller;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.autoconfigure.ExporterFactoryConfig;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.buildInfo.BuildInfo;
import gsrs.buildInfo.BuildInfoFetcher;
import gsrs.imports.MatchableCalculationConfig;
import gsrs.indexer.ConfigBasedIndexValueMakerFactory;
import gsrs.indexer.IndexValueMakerFactory;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.scheduler.GsrsSchedulerTaskPropertiesConfiguration;
import gsrs.util.RegisteredFunctionConfig;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.pojopointer.LambdaParseRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.GsrsFactoryConfiguration;
import gsrs.entityProcessor.EntityProcessorConfig;
import gsrs.imports.ImportAdapterFactoryConfig;
import gsrs.indexer.ConfigBasedIndexValueMakerConfiguration;
import gsrs.validator.ValidatorConfig;
import gsrs.indexer.IndexValueMakerFactory;


@RestController
public class BuildInfoController {

    // __aw__ come back to this, restore old build info, this is temporary 

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    @Autowired
    private BuildInfoFetcher buildInfoFetcher;


    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;

    @Autowired
    private ConfigBasedIndexValueMakerFactory configBasedIndexValueMakerFactory;

    @Autowired
    private IndexValueMakerFactory ivmf;


    @Autowired
    GsrsSchedulerTaskPropertiesConfiguration gsrsSchedulerTaskPropertiesConfiguration;

    @Autowired
    LambdaParseRegistry lambdaParseRegistry;


    @Autowired
    GsrsExportConfiguration gsrsExportConfiguration;

    @GetMapping("/api/v1/buildInfo")
    public BuildInfo getBuildInfo(){
        return buildInfoFetcher.getBuildInfo();
    }

    @GetMapping("/api/v1/@validatorConfigs")
    public JsonNode getFinishedValidatorConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends ValidatorConfig> list = gsrsFactoryConfiguration.getValidatorConfigByContext("substances");
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @GetMapping("/api/v1/@entityProcessorConfigs")
    public JsonNode getFinishedVEntityPorcessorConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends EntityProcessorConfig> list = gsrsFactoryConfiguration.getEntityProcessors();
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @GetMapping("/api/v1/@importAdapterFactoryConfigs")
    public JsonNode getImportAdapterFactoryConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends ImportAdapterFactoryConfig> list = gsrsFactoryConfiguration.getImportAdapterFactories("substances");
        JsonNode node = mapper.valueToTree(list);
        return node;
    }
    @GetMapping("/api/v1/@exporterFactoryConfigs")
    public JsonNode getExporterFactoryConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends ExporterFactoryConfig> list = gsrsExportConfiguration.getConfigs();
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @GetMapping("/api/v1/@matchableCalculationConfigs")
    public JsonNode getMatchableCalculationConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends MatchableCalculationConfig> list = gsrsFactoryConfiguration.getMatchableCalculationConfig("substances");
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @GetMapping("/api/v1/@scheduledTaskConfigs")
    public JsonNode getScheduledTaskConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends GsrsSchedulerTaskPropertiesConfiguration.ScheduledTaskConfig> list = gsrsSchedulerTaskPropertiesConfiguration.getConfigs();
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    // can't get this to work?
    @GetMapping("/api/v1/@registeredFunctionConfigs")
    public JsonNode getRegisteredFunctionConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends RegisteredFunctionConfig> list = lambdaParseRegistry.getConfigs();
        JsonNode node = mapper.valueToTree(list);
        return node;
    }



//    @GetMapping("/api/v1/@indexValueMakerConfigs")
//    public String getI() {
//        ObjectMapper mapper = new ObjectMapper();
//        TextIndexer defaultInstance = textIndexerFactory.getDefaultInstance();
//
//        String c = defaultInstance.toString();
//        if(textIndexerFactory.indexValueMakerFactory.getClass() == ConfigBasedIndexValueMakerFactory.class) {
//            defaultInstance.indexValueMakerFactory.
//        }
//        return c;
//    }

}
