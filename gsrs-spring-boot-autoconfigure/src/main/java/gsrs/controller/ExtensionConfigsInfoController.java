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

import javax.websocket.server.PathParam;
import java.util.List;
import java.util.Map;

@RestController
public class ExtensionConfigsInfoController {

    // These endpoints provide config objects that have been transformed from mapped
    // properties. So this represents the modified configuration that was supplied to GSRS code.
    // If everything goes smoothly after the final list was established, these should become
    // active. However, this data often depends on an initialization or cached supplier mechanism
    // that only kicks in under certain circumstances.  Sometime this is on application start;
    // other times it is a specific action. Therefore, you may get a null value or a blank response.

    // context (entity) is not always used but is needed for Gateway routing

    // Consider changing the endpoints to use service-info/api/v1/{service}/@xyzConfigs?entity=entity1

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
    @GetMapping("/api/v1/{context}/@validatorConfigs")
    public JsonNode getFinishedValidatorConfigs(@PathParam("context") String context) {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends ValidatorConfig> list = gsrsFactoryConfiguration.getValidatorConfigByContext(context);
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@entityProcessorConfigs")
    public JsonNode getFinishedVEntityProcessorConfigs(@PathParam("context") String context) {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends EntityProcessorConfig> list = gsrsFactoryConfiguration.getEntityProcessors();
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@importAdapterFactoryConfigs")
    public JsonNode getImportAdapterFactoryConfigs(@PathParam("context") String context) {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends ImportAdapterFactoryConfig> list = gsrsFactoryConfiguration.getImportAdapterFactories(context);
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@exporterFactoryConfigs")
    public JsonNode getExporterFactoryConfigs(@PathParam("context") String context) {
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
    @GetMapping("/api/v1/{context}/@matchableCalculationConfigs")
    public JsonNode getMatchableCalculationConfigs(@PathParam("context") String context) {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends MatchableCalculationConfig> list = gsrsFactoryConfiguration.getMatchableCalculationConfig(context);
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@scheduledTaskConfigs")
    public JsonNode getScheduledTaskConfigs(@PathParam("context") String context) {
        ObjectMapper mapper = new ObjectMapper();
        List<? extends GsrsSchedulerTaskPropertiesConfiguration.ScheduledTaskConfig> list = gsrsSchedulerTaskPropertiesConfiguration.getConfigs();
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@registeredFunctionConfigs")
    public JsonNode getRegisteredFunctionConfigs(@PathParam("context") String context) {
        // If no response, try viewing a chemical substance detail display in the UI.
        ObjectMapper mapper = new ObjectMapper();
        List<? extends RegisteredFunctionConfig> list = lambdaParseRegistry.reportConfigs();
        JsonNode node = mapper.valueToTree(list);
        return node;
    }

//  Not sure how to do this
//    @GetMapping("/api/v1/@indexValueMakerConfigs")
//    public String getIndexValueMakerConfigs() {
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