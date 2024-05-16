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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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


    @Value("#{new Boolean('${gsrs.extensions.config.report.api.enabled:false}')}")
    private boolean extensionsConfigReportApiEnabled;

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    @Autowired
    private IndexValueMakerFactory indexValueMakerFactory;

    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;

    @Autowired
    private ConfigBasedIndexValueMakerFactory configBasedIndexValueMakerFactory;

    @Autowired
    private GsrsSchedulerTaskPropertiesConfiguration gsrsSchedulerTaskPropertiesConfiguration;

    @Autowired
    private LambdaParseRegistry lambdaParseRegistry;

    @Autowired
    private GsrsExportConfiguration gsrsExportConfiguration;

    private ObjectMapper mapper = new ObjectMapper();

    private static final MediaType jmt = MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE);
    private static final String notEnabledMessage = "{ \"message\" : \"Resource Not Enabled.\"}";
    private static final String registeredFunctionsNotYetPopulated = "{\"message\": \"Registered " +
    "Functions may not yet be populated, try viewing a chemical substance details page in the UI \"}";
    private static final String exportFactoriesFunctionsNotYetPopulated = "{\"message\": " +
    "\"Exporters may not yet be populated for the context, try browsing the entity to cause the " +
    "exporterFactories to be populated. If that does not work, see the doc: 'How Configuration Works' " +
    "for a tip.\"}";

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@validatorConfigs")
    public ResponseEntity<?> getValidatorConfigs(@PathVariable("context") String context) {
        System.out.println("context: " + context);
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        List<? extends ValidatorConfig> list = gsrsFactoryConfiguration.getValidatorConfigByContext(context);
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@entityProcessorConfigs")
    public ResponseEntity<?> getFinishedVEntityProcessorConfigs(@PathVariable("context") String context) {
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        List<? extends EntityProcessorConfig> list = gsrsFactoryConfiguration.getEntityProcessors();
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@importAdapterFactoryConfigs")
    public ResponseEntity<?> getImportAdapterFactoryConfigs(@PathVariable("context") String context) {
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        List<? extends ImportAdapterFactoryConfig> list = gsrsFactoryConfiguration.getImportAdapterFactories(context);
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@exporterFactoryConfigs")
    public ResponseEntity<?> getExporterFactoryConfigs(@PathVariable("context") String context) {
       if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        Map<String, List<? extends ExporterFactoryConfig>> mapList = gsrsExportConfiguration.reportConfigs();
        if (mapList==null || mapList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).contentType(jmt).body(exportFactoriesFunctionsNotYetPopulated);
        }
        return ResponseEntity.status(HttpStatus.OK).body(mapList);
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@matchableCalculationConfigs")
    public ResponseEntity<?> getMatchableCalculationConfigs(@PathVariable("context") String context) {
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        List<? extends MatchableCalculationConfig> list = gsrsFactoryConfiguration.getMatchableCalculationConfig(context);
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@scheduledTaskConfigs")
    public ResponseEntity<?> getScheduledTaskConfigs(@PathVariable("context") String context) {
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        List<? extends GsrsSchedulerTaskPropertiesConfiguration.ScheduledTaskConfig> list = gsrsSchedulerTaskPropertiesConfiguration.getConfigs();
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @hasAdminRole
    @GetMapping("/api/v1/{context}/@registeredFunctionConfigs")
    public ResponseEntity<?> getRegisteredFunctionConfigs(@PathVariable("context") String context) {
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        List<? extends RegisteredFunctionConfig> list = lambdaParseRegistry.reportConfigs();
        if (list==null) {
            return ResponseEntity.status(HttpStatus.OK).contentType(jmt).body(registeredFunctionsNotYetPopulated);
        }
        return ResponseEntity.status(HttpStatus.OK).body(list);
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