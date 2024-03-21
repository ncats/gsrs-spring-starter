package gsrs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.GsrsFactoryConfiguration;
import gsrs.autoconfigure.ExporterFactoryConfig;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.entityProcessor.EntityProcessorConfig;
import gsrs.imports.ImportAdapterFactoryConfig;
import gsrs.imports.MatchableCalculationConfig;
import gsrs.indexer.ConfigBasedIndexValueMakerFactory;
import gsrs.indexer.IndexValueMakerFactory;
import gsrs.scheduler.GsrsSchedulerTaskPropertiesConfiguration;
import gsrs.security.hasAdminRole;
import gsrs.util.RegisteredFunctionConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.pojopointer.LambdaParseRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
public class ExtensionConfigurationController {

    // These endpoints provide config objects that have been transformed from mapped
    // properties. So this represents the configuration that were supplied to GSRS code.
    // If everything goes smoothly after the final list was established, these should become
    // active. However, this data often depends on an initialization or cached supplier mechanism
    // that only kicks in under certain circumstances.  Sometime this is on application start;
    // other times it is a specific action. Therefore, you may get a null value or a blank response.

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    @Autowired
    private IndexValueMakerFactory indexValueMakerFactory;

    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;

    @Autowired
    private ConfigBasedIndexValueMakerFactory configBasedIndexValueMakerFactory;

    @Autowired
    GsrsSchedulerTaskPropertiesConfiguration gsrsSchedulerTaskPropertiesConfiguration;

    @Autowired
    LambdaParseRegistry lambdaParseRegistry;

    @Autowired
    GsrsExportConfiguration gsrsExportConfiguration;

    @hasAdminRole
    @GetMapping("/api/v1/@validatorConfigs")
    public JsonNode getFinishedValidatorConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends ValidatorConfig> list = gsrsFactoryConfiguration.getValidatorConfigByContext("substances");
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @hasAdminRole
    @GetMapping("/api/v1/@entityProcessorConfigs")
    public JsonNode getFinishedVEntityPorcessorConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends EntityProcessorConfig> list = gsrsFactoryConfiguration.getEntityProcessors();
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @hasAdminRole
    @GetMapping("/api/v1/@importAdapterFactoryConfigs")
    public JsonNode getImportAdapterFactoryConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends ImportAdapterFactoryConfig> list = gsrsFactoryConfiguration.getImportAdapterFactories("substances");
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@exporterFactoryConfigs")
    public JsonNode getExporterFactoryConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<? extends ExporterFactoryConfig>> mapList = gsrsExportConfiguration.reportConfigs();
        JsonNode node;
        if (mapList == null || mapList.isEmpty()) {
            node = mapper.createObjectNode();
        } else {
            node = mapper.valueToTree(mapList);
        }
        return node;
    }

    @hasAdminRole
    @GetMapping("/api/v1/@matchableCalculationConfigs")
    public JsonNode getMatchableCalculationConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends MatchableCalculationConfig> list = gsrsFactoryConfiguration.getMatchableCalculationConfig("substances");
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @hasAdminRole
    @GetMapping("/api/v1/@scheduledTaskConfigs")
    public JsonNode getScheduledTaskConfigs() {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends GsrsSchedulerTaskPropertiesConfiguration.ScheduledTaskConfig> list = gsrsSchedulerTaskPropertiesConfiguration.getConfigs();
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @hasAdminRole
    @GetMapping("/api/v1/@registeredFunctionConfigs")
    public JsonNode getRegisteredFunctionConfigs() {
        // If no response, try viewing a chemical substance detail display in the UI.
        ObjectMapper mapper = new ObjectMapper();
        List<? extends RegisteredFunctionConfig> list = lambdaParseRegistry.reportConfigs();
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

//  Not sure how to do this
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