package gsrs.controller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.autoconfigure.ExpanderFactoryConfig;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.autoconfigure.ScrubberFactoryConfig;
import gsrs.repository.TextRepository;
import gsrs.security.GsrsSecurityUtils;
import ix.core.models.Text;
import ix.ginas.exporters.DefaultRecordExpanderFactory;
import ix.ginas.exporters.NoOpRecordScrubberFactory;
import ix.ginas.exporters.RecordExpanderFactory;
import ix.ginas.exporters.RecordScrubberFactory;
import ix.ginas.exporters.SpecificExporterSettings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractExportSupportingGsrsEntityController<C extends AbstractExportSupportingGsrsEntityController, T, I>
        extends AbstractLegacyTextSearchGsrsEntityController<C, T, I> {

    @Autowired
    protected TextRepository textRepository;
    

    @Autowired
    private GsrsExportConfiguration gsrsExportConfiguration;

    @Autowired
    protected PlatformTransactionManager transactionManager;
    
    CachedSupplier<List<Text>> exportSettingsPresets = CachedSupplier.of(()->{

    	List<Text> tlist = gsrsExportConfiguration.getHardcodedDefaultExportPresets(this.getEntityService().getContext());
    	if(tlist==null)return Collections.emptyList();
    	
    	return tlist;
    });

    @GetGsrsRestApiMapping({"/export/config({id})", "/export/config/{id}"})
    public ResponseEntity<Object> handleExportConfigFetch(@PathVariable("id") Long id,
                                                          @RequestParam Map<String, String> queryParameters) throws JsonProcessingException {
        log.trace("starting in handleExportConfigFetch");
        Objects.requireNonNull(id, "Must supply the ID of an existing export configuration");
        //todo: refactor to use new method
        Optional<Text> configurationHolder = textRepository.findById(id);
        Object returnObject;
        HttpStatus status;
        if (configurationHolder.isPresent()) {
            SpecificExporterSettings config = SpecificExporterSettings.fromText(configurationHolder.get());
            returnObject=config;
            status = HttpStatus.OK;
        } else {
            ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
            resultNode.put("Error", String.format("No configuration found with id %d", id));
            returnObject= resultNode;
            status=HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(returnObject, queryParameters), status);
    }

    protected Optional<SpecificExporterSettings> getConfigById(Long id){
        Optional<Text> configurationHolder = textRepository.findById(id);
        return configurationHolder.map(t->{
            try {
                return SpecificExporterSettings.fromText(t);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @GetGsrsRestApiMapping("/export/configs")
    public ResponseEntity<Object> handleExportConfigsFetch(
            @RequestParam Map<String, String> queryParameters) {
        log.trace("starting in handleExportConfigsFetch");
        String label = SpecificExporterSettings.getEntityKeyFromClass(getEntityService().getEntityClass().getName());
        List<Text> configs = textRepository.findByLabel(label);
        try {
            configs.addAll(getHardcodedConfigs());
        } catch (JsonProcessingException ex) {
            log.error("Error creating hard-coded exporter settings", ex);
        }

        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(configs.stream().map(t-> {
                    try {
                        return SpecificExporterSettings.fromText(t);
                    } catch (JsonProcessingException e) {
                        log.error("Error converting configuration value", e);
                    }
                    return null;
                }).collect(Collectors.toList()),
                queryParameters), HttpStatus.OK);
    }

    // todo:
    /*
    list every setting available for exporters
        find every single text value....
        for some entities,
        filter by entity type
        need entity specific keys
        ~20 preset defaults. Some for text exports, some for SD files... client can handle filtering
        we have many of these
        based on URL, expect substance-specific stuff
        option 1: 1 giant clob for all configs
        option 2:

     */
    //ExporterFactoryConfig
    @PostGsrsRestApiMapping("/export/config")
    public ResponseEntity<Object> handleExportConfigSave(@RequestBody String exportConfigJson,
                                                         @RequestParam Map<String, String> queryParameters) throws JsonProcessingException {
        log.trace("starting in handleExportConfigSave");
        ObjectMapper mapper = new ObjectMapper();
        SpecificExporterSettings conf = mapper.readValue(exportConfigJson, SpecificExporterSettings.class);
        if(doesConfigurationKeyExist(conf.getExporterKey())) {
            ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
            resultNode.put("Error in provided configuration",String.format("An Export configuration with key %s already exists in the database!",
                    conf.getExporterKey()));
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), HttpStatus.BAD_REQUEST);
        }
        conf.setEntityClass(getEntityService().getEntityClass().getName());
        conf.setOwner(GsrsSecurityUtils.getCurrentUsername().isPresent() ? GsrsSecurityUtils.getCurrentUsername().get() : "unknown");

        Text textObject = conf.asText();
        //deserialize exportConfigJson to DefaultExporterFactoryConfig
        //generate keys rather take user input
        // keys must be systematic

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Text savedText = transactionTemplate.execute(t -> textRepository.saveAndFlush(textObject));
        log.trace("completed save of Text with ID {}", savedText.id);
        conf.setConfigurationId(savedText.id.toString());
        savedText.setText(mapper.writeValueAsString(conf));
        savedText.setIsDirty("configurationId");
        log.trace("called savedText.setIsDirty(");
        TransactionTemplate transactionTemplate2 = new TransactionTemplate(transactionManager);
        transactionTemplate2.executeWithoutResult(t -> textRepository.saveAndFlush(savedText));
        log.trace("completed 2nd save of Text");
        //todo: ID processing

        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        resultNode.put("Newly created configuration", savedText.id);
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), HttpStatus.OK);
    }

    @DeleteGsrsRestApiMapping({"/export/config({id})", "/export/config/{id}"})
    public ResponseEntity<Object> handleExportConfigDelete(@PathVariable("id") Long id,
                                                           @RequestParam Map<String, String> queryParameters) {
        log.trace("starting in handleExportConfigFetch");
        Objects.requireNonNull(id, "Must supply the ID of an existing export configuration");


        Optional<Text> configurationHolder = textRepository.findById(id);
        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        HttpStatus status;
        if( !GsrsSecurityUtils.isAdmin()){
            resultNode.put("Error!", "Only admin users can delete an export configuration");
            status=HttpStatus.UNAUTHORIZED;
        } else if (configurationHolder.isPresent()) {
            TransactionTemplate transactionTemplateDelete = new TransactionTemplate(transactionManager);
            transactionTemplateDelete.executeWithoutResult(t -> textRepository.deleteByRecordId(id));
            resultNode.put("Result", String.format("Deleted export configuration with ID %d", id));
            status=HttpStatus.OK;
        } else {
            resultNode.put("Error!", String.format("No configuration found with id %d", id));
            status=HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), status);
    }

    @PutGsrsRestApiMapping({"/export/config({id})", "/export/config/{id}"})
    public ResponseEntity<Object> handleExportConfigUpdate(@PathVariable("id") Long id,
                                                           @RequestBody String exportConfigJson,
                                                           @RequestParam Map<String, String> queryParameters) throws JsonProcessingException {
        log.trace("starting in handleExportConfigUpdate");
        Objects.requireNonNull(id, "Must supply the ID of an existing export configuration");

        String currentUser = GsrsSecurityUtils.getCurrentUsername().isPresent() ? GsrsSecurityUtils.getCurrentUsername().get() : "unknown";

        Optional<Text> configurationHolder = textRepository.findById(id);
        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        HttpStatus status;
        if (configurationHolder.isPresent()) {
            Text retrievedText= configurationHolder.get();
            SpecificExporterSettings retrievedSettings = SpecificExporterSettings.fromText(retrievedText);
            if( retrievedSettings.getOwner() != null && retrievedSettings.getOwner().length()>0 && !retrievedSettings.getOwner().equals(currentUser) && !GsrsSecurityUtils.isAdmin()){
                resultNode.put("Error!", String.format("Attempt to update configuration created by %s", retrievedSettings.getOwner()));
                status=HttpStatus.UNAUTHORIZED;
            } else {
                Text textObj = new Text();
                textObj.setValue(exportConfigJson);
                SpecificExporterSettings exporterSettingsFromInput = SpecificExporterSettings.fromText(textObj);
                if( retrievedSettings.getOwner() == null || retrievedSettings.getOwner().length()==0){
                    log.info("retrieved export configuration without an owner; setting to current user ");
                    retrievedSettings.setOwner(currentUser);
                }
                exporterSettingsFromInput.setOwner(retrievedSettings.getOwner());
                exporterSettingsFromInput.setConfigurationId(id.toString());
                exportConfigJson = exporterSettingsFromInput.asText().getValue();
                retrievedText.setValue(exportConfigJson);
                log.trace("made call to setValue");
                TransactionTemplate transactionTemplateUpdate = new TransactionTemplate(transactionManager);
                String valueToSave =exportConfigJson;
                transactionTemplateUpdate.executeWithoutResult(t -> {
                    retrievedText.setIsAllDirty();
                    Text updated = textRepository.saveAndFlush(retrievedText);
                    if (updated == null) {
                        log.error("Error updating Text object!");
                    }
                    if (!Objects.equals(valueToSave, updated.getValue())) {
                        log.error("saved value is not what was supplied!");
                    }
                });
                resultNode.put("Result", String.format("Updated export configuration with ID %d", id));
                status = HttpStatus.OK;
            }
        } else {
            resultNode.put("Error!", String.format("No configuration found with id %d", id));
            status=HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), status);
    }

    @GetGsrsRestApiMapping("/export/scrubber/@schema")
    public ResponseEntity<Object> handleExportScrubberSchema(
            @RequestParam Map<String, String> queryParameters) throws IOException {
        log.trace("starting in handleExportScrubberSchema.  Using scrubber factory {}",
                getScrubberFactory().getClass().getName());

        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(getScrubberFactory().getSettingsSchema(),
                queryParameters), HttpStatus.OK);
    }

    @GetGsrsRestApiMapping("/export/expander/@schema")
    public ResponseEntity<Object> handleExportExpanderSchema(
            @RequestParam Map<String, String> queryParameters) throws IOException {
        log.trace("starting in handleExportExpanderSchema. expander factory: {}", getExpanderFactory().getClass().getName());

        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(getExpanderFactory().getSettingsSchema(), queryParameters), HttpStatus.OK);
    }

    boolean doesConfigurationKeyExist(String configurationKey) {
        Class entityClass = getEntityService().getEntityClass();
        Objects.requireNonNull(entityClass, "Must be able to resolver the entity class");
        String label = SpecificExporterSettings.getEntityKeyFromClass(entityClass.getName());
        List<Text> configs = textRepository.findByLabel(label);
        return configs.stream().anyMatch(c->{
            SpecificExporterSettings config = null;
            try {
                config = SpecificExporterSettings.fromText(c);
            } catch (JsonProcessingException e) {
                log.error("Error");
            }
            return config.getConfigurationKey()!= null && config.getConfigurationKey().equalsIgnoreCase(configurationKey);
        });
    }

    public RecordScrubberFactory<T> getScrubberFactory(){
        return new NoOpRecordScrubberFactory<T>();
    }

    public RecordExpanderFactory<T> getExpanderFactory(){
        return new DefaultRecordExpanderFactory<>();
    }

    public RecordExpanderFactory<T> getExpanderFactory(ExpanderFactoryConfig config) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        log.trace("Going to create an expander factory of type: {}", config.getExpanderFactory().getName());
        return (RecordExpanderFactory<T>) config.getExpanderFactory().getConstructor().newInstance();
    }

    public RecordScrubberFactory<T> getScrubberFactory(ScrubberFactoryConfig config) throws  NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        return (RecordScrubberFactory<T>) config.getScrubberFactoryClass().getConstructor().newInstance();
    }

    /*
    Items that will be of general usage
     */
    public List<Text> getHardcodedConfigs() throws JsonProcessingException {
        return exportSettingsPresets.get();
    }
}
