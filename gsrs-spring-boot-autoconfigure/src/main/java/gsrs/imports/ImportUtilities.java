package gsrs.imports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.dataexchange.model.ProcessingAction;
import gsrs.dataexchange.model.ProcessingActionConfig;
import gsrs.dataexchange.model.ProcessingActionConfigSet;
import gsrs.dataexchange.services.ImportMetadataReindexer;
import gsrs.repository.TextRepository;
import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import gsrs.stagingarea.model.ImportData;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.model.MatchedRecordSummary;
import gsrs.stagingarea.service.StagingAreaService;
import ix.core.models.Text;
import ix.core.util.EntityUtils;
import ix.ginas.exporters.SpecificExporterSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class ImportUtilities<T> {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private GsrsImportAdapterFactoryFactory gsrsImportAdapterFactoryFactory;

    @Autowired
    public PayloadService payloadService;

    private String contextName ="";

    private Class<T> entityClass;

    private static final Pattern regExGuid= Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

    private final static ExecutorService executor = Executors.newFixedThreadPool(1);


    public ImportUtilities(String contextName, Class<T> entityClass) {
        this.contextName=contextName;
        this.entityClass=entityClass;
    }
    private final CachedSupplier<List<ImportAdapterFactory<T>>> importAdapterFactories
            = CachedSupplier.of(() -> gsrsImportAdapterFactoryFactory.newFactory(this.contextName,
            entityClass));

    public static void enhanceWithMetadata(ObjectNode dataNode, ImportMetadata metadata, StagingAreaService service) {
        ObjectMapper mapper = new ObjectMapper();
        if (metadata != null) {
            String metadataAsString;
            try {
                EntityUtils.EntityInfo<ImportMetadata> eics= EntityUtils.getEntityInfoFor(ImportMetadata.class);
                if (metadata.validations == null || metadata.validations.isEmpty()) {
                    log.trace("going to fill in validations");
                    service.fillCollectionsForMetadata(metadata);
                }
                metadataAsString = mapper.writeValueAsString(metadata);
                ImportMetadata copy = eics.fromJson(metadataAsString);
                log.trace("starting filtering lambda. copy has {} kvms", copy.getKeyValueMappings().size());
                copy.setKeyValueMappings(metadata.getKeyValueMappings().stream().filter(kv->!kv.getRecordId().equals(metadata.getRecordId())).collect(Collectors.toList()));
                log.trace("completed filtering lambda. copy has {} kvms", copy.getKeyValueMappings().size());
                JsonNode metadataAsNode = mapper.readTree(mapper.writeValueAsString(copy));
                dataNode.set("_metadata", metadataAsNode);

                MatchedRecordSummary matchedRecordSummary = service.findMatches(metadata);
                log.trace("computed matches");
                JsonNode matchedRecordsAsNode = mapper.readTree(mapper.writeValueAsString(matchedRecordSummary));
                dataNode.set("_matches", matchedRecordsAsNode);

            } catch (ClassNotFoundException | IOException e) {
                log.error("Error processing metadata", e);
                throw new RuntimeException(e);
            }

        } else {
            dataNode.put("_metadata", "[not found]");
        }
    }

    public static List<AbstractImportSupportingGsrsEntityController.ImportTaskMetaData> getAllImportTasks(Class<?> entityClass, TextRepository textRepository) {
        log.trace("getAllImportTasks");
        List<AbstractImportSupportingGsrsEntityController.ImportTaskMetaData> allImportConfigs = new ArrayList<>();
        Objects.requireNonNull(entityClass, "Must be able to resolve the entity class");

        String label = AbstractImportSupportingGsrsEntityController.ImportTaskMetaData.getEntityKeyFromClass(entityClass.getName());
        List<Text> configs = textRepository.findByLabel(label);
        log.trace("total configs: {}", configs.size());
        configs.forEach(c -> {
            AbstractImportSupportingGsrsEntityController.ImportTaskMetaData config;
            try {
                config = AbstractImportSupportingGsrsEntityController.ImportTaskMetaData.fromText(c);
                allImportConfigs.add(config);
            } catch (JsonProcessingException e) {
                log.error("Error in getAllImportTasks", e);
            }
        });
        return allImportConfigs;
    }

    public static String removeMetadataFromDomainObjectJson(String domainObjectJson) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode objectAsNode = mapper.readTree(domainObjectJson);
            if (objectAsNode.hasNonNull("_metadata") && objectAsNode.isObject()) {
                ((ObjectNode) objectAsNode).remove("_metadata");
            }
            if (objectAsNode.hasNonNull("_matches") && objectAsNode.isObject()) {
                ((ObjectNode) objectAsNode).remove("_matches");
            }
            return objectAsNode.toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Sort parseSortFromOrderParam(String order) {
        //match Gsrs Play API
        if (order == null || order.trim().isEmpty()) {
            return Sort.sort(ImportMetadata.class);
        }
        char firstChar = order.charAt(0);
        if ('$' == firstChar) {
            return Sort.by(Sort.Direction.DESC, order.substring(1));
        }
        if ('^' == firstChar) {
            return Sort.by(Sort.Direction.ASC, order.substring(1));
        }
        return Sort.by(Sort.Direction.ASC, order);
    }

    public static boolean doesImporterKeyExist(String importerId, Class<?> entityClass, TextRepository textRepository) {
        log.trace("doesImporterKeyExist");
        Objects.requireNonNull(entityClass, "Must be able to resolve the entity class");

        String label = AbstractImportSupportingGsrsEntityController.ImportTaskMetaData.getEntityKeyFromClass(entityClass.getName());
        List<Text> configs = textRepository.findByLabel(label);
        log.trace("total configs: {}", configs.size());
        return configs.stream().anyMatch(c -> {
            SpecificExporterSettings config = null;
            try {
                config = SpecificExporterSettings.fromText(c);
            } catch (JsonProcessingException e) {
                log.error("Error", e);
            }
            return config.getExporterKey() != null && config.getExporterKey().equalsIgnoreCase(importerId);
        });
    }

    public ObjectNode handleAction(StagingAreaService stagingAreaService, String matchedEntityId, String stagingRecordId,
                                        int version, String persist, String processingJson, String contextName ) throws Exception {
        assert stagingAreaService != null;
        ObjectMapper mapper = new ObjectMapper();
        ProcessingActionConfigSet configSet = mapper.readValue(processingJson, ProcessingActionConfigSet.class);
        return processOneRecord(stagingAreaService, stagingRecordId, matchedEntityId, version, persist, configSet.getProcessingActions(),
                contextName);
    }

    public List<ObjectNode> handleActions(StagingAreaService stagingAreaService,
                                   int version, String persist, String processingJson, String contextName ) throws Exception {
        assert stagingAreaService != null;
        ObjectMapper mapper = new ObjectMapper();
        ProcessingActionConfigSet configSet = mapper.readValue(processingJson, ProcessingActionConfigSet.class);
        List<ObjectNode> returnNodes = new ArrayList<>();
        for(int r =0; r<configSet.getStagingAreaRecords().size();r++) {
            String stagingAreaId = configSet.getStagingAreaRecords().get(r).getId();
            String databaseRecordId= configSet.getStagingAreaRecords().get(r).getMatchingID();
            log.trace("matched ids {} and {}", stagingAreaId, databaseRecordId);
            ObjectNode singleReturn = processOneRecord(stagingAreaService, stagingAreaId, databaseRecordId, version, persist,
                    configSet.getProcessingActions(), contextName);
            returnNodes.add(singleReturn);
        }
        return returnNodes;
    }

    private ObjectNode processOneRecord(StagingAreaService stagingAreaService, String stagingRecordId, String matchedEntityId,
                                        int version, String persist, List<ProcessingActionConfig> processingActions,
                                        String context) throws Exception {
        ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
        if(stagingRecordId==null || stagingRecordId.length()==0){
            messageNode.put("message", "blank input");
            messageNode.put("status", HttpStatus.BAD_REQUEST.value());
            return messageNode;
        }
        Matcher matcher = regExGuid.matcher(stagingRecordId);
        if(!matcher.find()){
            messageNode.put("message", "input is not a valid id");
            messageNode.put("status", HttpStatus.BAD_REQUEST.value());
            return messageNode;
        }

        ImportData importData = stagingAreaService.getImportDataByInstanceIdOrRecordId(stagingRecordId, version);
        UUID recordId = null;

        String objectJson = "";
        String objectClass = "";

        if (importData != null) {
            log.trace("Data for id {} retrieved", stagingRecordId);
            objectJson = importData.getData();
            objectClass = importData.getEntityClassName();
            recordId = importData.getRecordId();
            log.trace("looking for data for record id {} - instance id {}", importData.getRecordId(),
                    importData.getInstanceId());
            ImportMetadata metadata = stagingAreaService.getImportMetaData(recordId.toString(), 0);
            //todo: figure out whether to do the same for records marked 'merged'
            if (metadata.getImportStatus() == ImportMetadata.RecordImportStatus.imported) {
                messageNode.put("message", String.format("Error: staging area record with ID %s has already been imported",
                        stagingRecordId));
                messageNode.put("status", HttpStatus.BAD_REQUEST.value());
                messageNode.put("stagingAreaId", stagingRecordId);
                return messageNode;
            }
        }
        log.trace("objectJson: {}", objectJson);
        if (objectJson == null || objectJson.length() == 0) {
            messageNode.put("message", String.format("Error retrieving staging area object of type %s with ID %s",
                    objectClass, stagingRecordId));
            messageNode.put("status", HttpStatus.BAD_REQUEST.value());
            messageNode.put("stagingAreaId", stagingRecordId);
            return messageNode;
        }

        //make sure class type is the same as the stagingAreaService's entity name!
        log.trace("going to retrieve existing object of type {} with ID {}", objectClass, matchedEntityId);
        T baseObject = null;
        if(matchedEntityId!=null && matchedEntityId.length()>0) {
            baseObject = stagingAreaService.retrieveEntity(objectClass, matchedEntityId);
        }

        T currentObject = stagingAreaService.deserializeObject(objectClass, objectJson);
        if (currentObject == null) {
            messageNode.put("message", String.format("Error retrieving imported object of type %s with ID %s",
                    objectClass, matchedEntityId));
            messageNode.put("status", HttpStatus.BAD_REQUEST.value());
            messageNode.put("stagingAreaId", stagingRecordId);
            return messageNode;
        }

        StringBuilder whatHappened = new StringBuilder();
        whatHappened.append("Processing record: ");

        ImportMetadata.RecordImportStatus recordImportStatus = ImportMetadata.RecordImportStatus.staged;
        boolean savingNewItem = false;
        for (ProcessingActionConfig configItem : processingActions) {
            ProcessingAction action =gsrsImportAdapterFactoryFactory.getMatchingProcessingAction(context, configItem.getProcessingActionName());
            if(action == null ){
                log.error("action {} not found!", configItem.getProcessingActionName());
                continue;
            }

            log.trace("going to call action {}", action.getClass().getName());
            currentObject = (T) action.process(currentObject, baseObject, configItem.getParameters(), whatHappened::append);
            //todo: check this logic
            if (action.getClass().getName().toUpperCase().contains("MERGE") ||
                    action.getClass().getName().toUpperCase().contains("CREATEBATCH")) {
                recordImportStatus = ImportMetadata.RecordImportStatus.merged;
            } else if (action.getClass().getName().toUpperCase().contains("REPLACE")
                    || action.getClass().getName().toUpperCase().contains("CREATE")) {
                savingNewItem = true;
                recordImportStatus = ImportMetadata.RecordImportStatus.imported;
            } else if (action.getClass().getName().toUpperCase().contains("IGNORE")
                    || action.getClass().getName().toUpperCase().contains("REJECT")) {
                recordImportStatus = ImportMetadata.RecordImportStatus.rejected;
            }
        }
        log.trace(whatHappened.toString());
        if (persist != null && persist.equalsIgnoreCase("TRUE") && isSaveNecessary(recordImportStatus)) {
            //check whether we have something to save... otherwise, skip to the next step in the processing
            if(currentObject!=null ) {
                GsrsEntityService.ProcessResult<T> result = stagingAreaService.saveEntity(objectClass, currentObject, savingNewItem);
                if (!result.isSaved()) {
                    log.error("Error! Saved object is null");
                    messageNode.put("message", "Object failed to save");
                    messageNode.put("error", result.getThrowable().getMessage());
                    messageNode.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
                    return messageNode;
                }
                currentObject = result.getEntity();
                log.trace("saved new or updated entity");

            }
            stagingAreaService.updateRecordImportStatus(recordId, recordImportStatus);
            UUID indexingEventId = UUID.randomUUID();
            ImportMetadata metadata = stagingAreaService.getImportMetaData(recordId.toString(), 0);
            EntityUtils.EntityWrapper<ImportMetadata> wrappedObject = EntityUtils.EntityWrapper.of(metadata);
            ImportMetadataReindexer.indexOneItem(indexingEventId, eventPublisher::publishEvent, EntityUtils.Key.of(wrappedObject),
                    EntityUtils.EntityWrapper.of(wrappedObject));
            log.trace("reindexing importmetadata object");
        } else {
            log.trace("skipped saving/processing");
        }
        ObjectMapper mapper = new ObjectMapper();
        if( currentObject!=null) {
            //messageNode.put("object", mapper.writeValueAsString(currentObject));
            messageNode.put("message", "Import record processed successfully");
            messageNode.put("stagingAreaId", stagingRecordId);
            messageNode.put("status", HttpStatus.OK.value());
            return messageNode;
        }else{
            messageNode.put("message", "Import record processed successfully");
            messageNode.put("stagingAreaId", stagingRecordId);
            messageNode.put("status", HttpStatus.OK.value());
            return messageNode;
        }
    }

    public static boolean isSaveNecessary(ImportMetadata.RecordImportStatus status) {
        return status == ImportMetadata.RecordImportStatus.imported || status == ImportMetadata.RecordImportStatus.merged;
    }

    protected ImportAdapterFactory<T> fetchAdapterFactory(AbstractImportSupportingGsrsEntityController.ImportTaskMetaData<T> task) throws Exception {
        if (task.getAdapter() == null) {
            throw new IOException("Cannot predict settings with null import adapter");
        }
        ImportAdapterFactory<T> adaptFac = getImportAdapterFactory(task.getAdapter())
                .orElse(null);
        if (adaptFac == null) {
            throw new IOException("Cannot predict settings with unknown import adapter:\"" + task.getAdapter() + "\"");
        }
        log.trace("in fetchAdapterFactory, adaptFac: {}", adaptFac);
        log.trace("in fetchAdapterFactory, adaptFac: {}, ", adaptFac.getClass().getName());
        log.trace("in fetchAdapterFactory, staging area service: {}", adaptFac.getStagingAreaService().getName());
        adaptFac.setFileName(task.getFilename());
        return adaptFac;
    }


    public List<ImportAdapterFactory<T>> getImportAdapters() {
        return importAdapterFactories.get();
    }

    public Optional<ImportAdapterFactory<T>> getImportAdapterFactory(String name) {
        log.trace("In getImportAdapterFactory, looking for adapter with name '{}' among {}", name, getImportAdapters().size());
        if (getImportAdapters() != null) {
            getImportAdapters().forEach(a -> log.trace("adapter with name: '{}', key: '{}'", a.getAdapterName(), a.getAdapterKey()));
        }
        Optional<ImportAdapterFactory<T>> adapterFactory = getImportAdapters().stream().filter(n -> name.equals(n.getAdapterName())).findFirst();
        if (!adapterFactory.isPresent()) {
            log.trace("searching for adapter by name failed; using key");
            adapterFactory = getImportAdapters().stream().filter(n -> name.equals(n.getAdapterKey())).findFirst();
            log.trace("success? {}", adapterFactory.isPresent());
        }
        return adapterFactory;
    }

}
