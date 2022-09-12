package gsrs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.repository.TextRepository;
import ix.core.models.Text;
import ix.ginas.exporters.DefaultExporterFactoryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public abstract class AbstractExportSupportingGsrsEntityController<C extends AbstractExportSupportingGsrsEntityController, T, I>
        extends AbstractLegacyTextSearchGsrsEntityController<C, T, I> {

    @Autowired
    private TextRepository textRepository;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @GetGsrsRestApiMapping({"/export/config({id})", "/export/config/{id}"})
    public ResponseEntity<Object> handleExportConfigFetch(@PathVariable("id") Long id,
                                                          @RequestParam Map<String, String> queryParameters) {
        log.trace("starting in handleExportConfigFetch");
        Objects.requireNonNull(id, "Must supply the ID of an existing export configuration");
        Optional<Text> configurationHolder = textRepository.findById(id);
        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        if (configurationHolder.isPresent()) {
            resultNode.put("Requested configuration", configurationHolder.get().getValue());
        } else {
            resultNode.put("Error!", String.format("No configuration found with id %d", id));
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), HttpStatus.OK);
    }

    @GetGsrsRestApiMapping("/export/configs")
    public ResponseEntity<Object> handleExportConfigsFetch(
            @RequestParam Map<String, String> queryParameters) {
        log.trace("starting in handleExportConfigsFetch");
        String label = DefaultExporterFactoryConfig.getEntityKeyFromClass(getEntityService().getEntityClass().getName());
        List<Text> configs = textRepository.findByLabel(label);
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(configs, queryParameters), HttpStatus.OK);
    }

    // todo:
    /*
    need a get for all configs
    return JSON for all of them
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
        DefaultExporterFactoryConfig conf = mapper.readValue(exportConfigJson, DefaultExporterFactoryConfig.class);
        conf.setEntityClass(getEntityService().getEntityClass().getName());

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
        if (configurationHolder.isPresent()) {
            TransactionTemplate transactionTemplateDelete = new TransactionTemplate(transactionManager);
            transactionTemplateDelete.executeWithoutResult(t -> textRepository.deleteByRecordId(id));
            resultNode.put("Result", String.format("Deleted export configuration with ID %d", id));
        } else {
            resultNode.put("Error!", String.format("No configuration found with id %d", id));
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), HttpStatus.OK);
    }

    @PutGsrsRestApiMapping({"/export/config({id})", "/export/config/{id}"})
    public ResponseEntity<Object> handleExportConfigUpdate(@PathVariable("id") Long id,
                                                           @RequestBody String exportConfigJson,
                                                           @RequestParam Map<String, String> queryParameters) throws JsonProcessingException {
        log.trace("starting in handleExportConfigUpdate");
        Objects.requireNonNull(id, "Must supply the ID of an existing export configuration");

        Optional<Text> configurationHolder = textRepository.findById(id);
        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        if (configurationHolder.isPresent()) {
            Text retrievedText= configurationHolder.get();
            retrievedText.setValue(exportConfigJson);
            log.trace("made call to setValue");
            TransactionTemplate transactionTemplateUpdate = new TransactionTemplate(transactionManager);
            transactionTemplateUpdate.executeWithoutResult(t ->{
                retrievedText.setIsAllDirty();
                Text updated= textRepository.saveAndFlush(retrievedText);
                if(updated== null) {
                    log.error("Error updating Text object!");
                }
                if(!Objects.equals(exportConfigJson,updated.getValue())) {
                    log.error("saved value is not what was supplied!");
                }
            });
            resultNode.put("Result", String.format("Updated export configuration with ID %d", id));
        } else {
            resultNode.put("Error!", String.format("No configuration found with id %d", id));
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), HttpStatus.OK);
    }

}
