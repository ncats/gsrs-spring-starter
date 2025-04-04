package gsrs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.GsrsFactoryConfiguration;
import gsrs.autoconfigure.ExporterFactoryConfig;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.entityProcessor.EntityProcessorConfig;
import gsrs.imports.ImportAdapterFactoryConfig;
import gsrs.imports.MatchableCalculationConfig;
import gsrs.indexer.ConfigBasedIndexValueMakerConfiguration;
import gsrs.indexer.ConfigBasedIndexValueMakerFactory;
import gsrs.indexer.IndexValueMakerFactory;
import gsrs.scheduler.GsrsSchedulerTaskPropertiesConfiguration;
import gsrs.security.hasAdminRole;
import gsrs.util.RegisteredFunctionConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.search.text.TextIndexerConfig;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.pojopointer.LambdaParseRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@Profile("!test")
public class ExtensionConfigsInfoController {

    // These endpoints provide config objects that have been transformed from mapped
    // properties. So this represents the modified configuration that was supplied to GSRS code.
    // If everything goes smoothly after the final list was established, these should become
    // active. However, this data often depends on an initialization or cached supplier mechanism
    // that only kicks in under certain circumstances.  Sometime this is on application start;
    // other times it is a specific action. Therefore, you may get a null value or a blank response.

    // entityContext is not always, depending on whether the extension puts configs
    // into buckets by entity.


    @Value("#{new Boolean('${gsrs.extensions.config.report.api.enabled:false}')}")
    private boolean extensionsConfigReportApiEnabled;

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    @Autowired
    private IndexValueMakerFactory indexValueMakerFactory;

    @Autowired
    private TextIndexerConfig textIndexerConfig;

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
    private static final String noDataMessage = "{\"message\": \"A null or empty value was returned, or " +
    "an error occurred. This can happen if configs have not yet been populated or if there are no config " +
    "objects corresponding to the serviceContext (and/or entityContext) provided. In some cases, API " +
    "actions trigger the population of the data into cached suppliers or autowired values. These values " +
    "are null or empty until populated. In other cases, data is populated when the service starts. See " +
    "the doc: 'How Configuration Works' for some more detail.\"}";


    @hasAdminRole
    @GetMapping("/service-info/api/v1/{serviceContext}/@validatorConfigs/{entityContext}")
    public ResponseEntity<?> getValidatorConfigs(
        @PathVariable("serviceContext") String serviceContext,
        @PathVariable("entityContext") String entityContext
    ) {
        List<? extends ValidatorConfig> list = null;
        boolean thrown = false;
        try {
            list = gsrsFactoryConfiguration.getValidatorConfigByContext(entityContext);
        } catch (Throwable t) {
            thrown = true;
        }
        if (thrown || list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).contentType(jmt).body(noDataMessage);
        }
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @hasAdminRole
    @GetMapping("/service-info/api/v1/{serviceContext}/@entityProcessorConfigs")
    public ResponseEntity<?> getFinishedVEntityProcessorConfigs(
        @PathVariable("serviceContext") String serviceContext
    ) {
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        List<? extends EntityProcessorConfig> list = gsrsFactoryConfiguration.getEntityProcessors();
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @hasAdminRole
    @GetMapping("/service-info/api/v1/{serviceContext}/@importAdapterFactoryConfigs/{entityContext}")
    public ResponseEntity<?> getImportAdapterFactoryConfigs(
        @PathVariable("serviceContext") String serviceContext,
        @PathVariable("entityContext") String entityContext
    ) {
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        List<? extends ImportAdapterFactoryConfig> list = null;
        boolean thrown = false;
        try {
            list = gsrsFactoryConfiguration.getImportAdapterFactories(entityContext);
        } catch (Throwable t) {
            thrown = true;
        }
        if (thrown || list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).contentType(jmt).body(noDataMessage);
        }
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @hasAdminRole
    @GetMapping("/service-info/api/v1/{serviceContext}/@exporterFactoryConfigs")
    public ResponseEntity<?> getExporterFactoryConfigs(
        @PathVariable("serviceContext") String serviceContext
    ) {
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        boolean thrown = false;
        Map<String, List<? extends ExporterFactoryConfig>> mapList = null;
        try {
            mapList = gsrsExportConfiguration.reportConfigs();
        } catch (Throwable t) {
            thrown = true;
        }
        if (thrown || mapList == null || mapList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).contentType(jmt).body(noDataMessage);
        }
        return ResponseEntity.status(HttpStatus.OK).body(mapList);
    }

    @hasAdminRole
    @GetMapping("/service-info/api/v1/{serviceContext}/@matchableCalculationConfigs/{entityContext}")
    public ResponseEntity<?> getMatchableCalculationConfigs(
        @PathVariable("serviceContext") String serviceContext,
        @PathVariable("entityContext") String entityContext
    ) {
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        List<? extends MatchableCalculationConfig> list = null;
        boolean thrown = false;
        try {
            list = gsrsFactoryConfiguration.getMatchableCalculationConfig(entityContext);
        } catch (Throwable t) {
            thrown = true;
        }
        if (thrown || list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).contentType(jmt).body(noDataMessage);
        }
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @hasAdminRole
    @GetMapping("/service-info/api/v1/{serviceContext}/@scheduledTaskConfigs")
    public ResponseEntity<?> getScheduledTaskConfigs(
        @PathVariable("serviceContext") String serviceContext
    ) {
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        List<? extends GsrsSchedulerTaskPropertiesConfiguration.ScheduledTaskConfig> list = null;
        boolean thrown = false;
        try {
            list = gsrsSchedulerTaskPropertiesConfiguration.getConfigs();
        } catch (Throwable t) {
            thrown = true;
        }
        if (thrown || list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).contentType(jmt).body(noDataMessage);
        }
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @hasAdminRole
    @GetMapping("/service-info/api/v1/{serviceContext}/@registeredFunctionConfigs")
    public ResponseEntity<?> getRegisteredFunctionConfigs(
        @PathVariable("serviceContext") String serviceContext
    ) {
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        List<? extends RegisteredFunctionConfig> list = null;
        boolean thrown = false;
        try {
            list = lambdaParseRegistry.reportConfigs();
        } catch (Throwable t) {
            thrown = true;
        }
        if (thrown || list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).contentType(jmt).body(noDataMessage);
        }
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @hasAdminRole
    @GetMapping("/service-info/api/v1/{serviceContext}/@indexValueMakerConfigs")
    public ResponseEntity<?> getIndexValueMakerConfigs() {
        List<ConfigBasedIndexValueMakerConfiguration.IndexValueMakerConf> list = null;
        if (!extensionsConfigReportApiEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notEnabledMessage);
        }
        if (textIndexerFactory.indexValueMakerFactory.getClass() == ConfigBasedIndexValueMakerFactory.class) {
            boolean thrown = false;
            try {
                list = configBasedIndexValueMakerFactory.getConfList();
            } catch (Throwable t) {
                thrown = true;
            }
            if (thrown || list == null || list.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK).contentType(jmt).body(noDataMessage);
            }
            return ResponseEntity.status(HttpStatus.OK).body(list);
        } else {
            String unexpectedClassMessage = "{ \"message\" : \"Expected IndexValueMakerFactory class to be: "+ ConfigBasedIndexValueMakerFactory.class + ". But, this was not the case.\"}";
            return ResponseEntity.status(HttpStatus.OK).contentType(jmt).body(unexpectedClassMessage);
        }
    }
}