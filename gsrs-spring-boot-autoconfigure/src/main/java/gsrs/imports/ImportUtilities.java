package gsrs.imports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.common.util.Unchecked;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.dataexchange.model.ImportProcessingJob;
import gsrs.dataexchange.model.ProcessingAction;
import gsrs.dataexchange.model.ProcessingActionConfig;
import gsrs.dataexchange.model.ProcessingActionConfigSet;
import gsrs.dataexchange.services.ImportMetadataReindexer;
import gsrs.indexer.IndexRemoveEntityEvent;
import gsrs.repository.PrincipalRepository;
import gsrs.repository.TextRepository;
import gsrs.security.AdminService;
import gsrs.security.GsrsSecurityUtils;
import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import gsrs.stagingarea.model.ImportData;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.model.ImportRecordParameters;
import gsrs.stagingarea.model.MatchedRecordSummary;
import gsrs.stagingarea.repository.ImportProcessingJobRepository;
import gsrs.stagingarea.service.StagingAreaService;
import ix.core.EntityFetcher;
import ix.core.models.Principal;
import ix.core.models.Text;
import ix.core.search.text.TextIndexerEntityListener;
import ix.core.util.EntityUtils;
import ix.core.validator.ValidationMessage;
import ix.ginas.exporters.SpecificExporterSettings;
import lombok.extern.slf4j.Slf4j;
import org.jcvi.jillion.core.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ImportUtilities<T> {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private GsrsImportAdapterFactoryFactory gsrsImportAdapterFactoryFactory;

    @Autowired
    public PayloadService payloadService;

    @Autowired
    private ImportProcessingJobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AdminService adminService;

    @Autowired
    private TextIndexerEntityListener textIndexerEntityListener;

    @Autowired
    PrincipalRepository principalRepository;

    private String contextName;

    private Class<T> entityClass;

    private static final Pattern regExGuid= Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

    private final static ExecutorService executor = Executors.newFixedThreadPool(1);

    StagingAreaService stagingAreaService;

    public ImportUtilities(String contextName, Class<T> entityClass, StagingAreaService service) {
        this.contextName=contextName;
        this.entityClass=entityClass;
        this.stagingAreaService=service;
    }

    //for a unit test - not general use
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
                log.trace("before changed code");
                if (metadata.getValidations() == null || metadata.getValidations().isEmpty()) {
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
            assert config != null;
            return config.getExporterKey() != null && config.getExporterKey().equalsIgnoreCase(importerId);
        });
    }

    public ObjectNode handleAction(StagingAreaService stagingAreaService, String matchedEntityId, String stagingRecordId,
                                        int version, String persist, String processingJson, String contextName ) throws Exception {
        assert stagingAreaService != null;
        ObjectMapper mapper = new ObjectMapper();
        ProcessingActionConfigSet configSet = mapper.readValue(processingJson, ProcessingActionConfigSet.class);
        return processOneRecord(stagingAreaService, stagingRecordId, matchedEntityId, version, persist, configSet.getProcessingActions());
    }

    public List<ObjectNode> handleActions(StagingAreaService stagingAreaService,
                                   int version, String persist, String processingJson) throws Exception {
        assert stagingAreaService != null;
        ObjectMapper mapper = new ObjectMapper();
        ProcessingActionConfigSet configSet = mapper.readValue(processingJson, ProcessingActionConfigSet.class);
        List<ObjectNode> returnNodes = new ArrayList<>();
        for(int r =0; r<configSet.getStagingAreaRecords().size();r++) {
            String stagingAreaId = configSet.getStagingAreaRecords().get(r).getId();
            String databaseRecordId= configSet.getStagingAreaRecords().get(r).getMatchingID();
            log.trace("matched ids {} and {}", stagingAreaId, databaseRecordId);
            ObjectNode singleReturn = processOneRecord(stagingAreaService, stagingAreaId, databaseRecordId, version, persist,
                    configSet.getProcessingActions());
            returnNodes.add(singleReturn);
        }
        return returnNodes;
    }

    public JsonNode handleActionsAsync(StagingAreaService stagingAreaService,
                                                  int version, String persist, String processingJson) throws Exception {

        assert stagingAreaService != null;
        ObjectMapper mapper = new ObjectMapper();
        ProcessingActionConfigSet configSet = mapper.readValue(processingJson, ProcessingActionConfigSet.class);
        ImportProcessingJob job = new ImportProcessingJob();
        job.setId(UUID.randomUUID());
        job.setStartDate(DateUtil.getCurrentDate());
        job.setJobData(processingJson);
        job.setJobStatus("starting");
        job.setStatusMessage("Processing started");
        job.setTotalRecords(configSet.getStagingAreaRecords().size());
        job.setCompletedRecordCount(0);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(r-> jobRepository.saveAndFlush(job));
        log.trace("handleActionsAsync create job {}", job);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ArrayNode[] returnFromInnerLambda = new ArrayNode[1];
        executor.execute(()-> {
            ArrayNode returnNodes = JsonNodeFactory.instance.arrayNode();
            for (int r = 0; r < configSet.getStagingAreaRecords().size(); r++) {
                String stagingAreaId = configSet.getStagingAreaRecords().get(r).getId();
                String databaseRecordId = configSet.getStagingAreaRecords().get(r).getMatchingID();
                log.trace("processing ids {} and {}", stagingAreaId, databaseRecordId);
                final ObjectNode[] singleReturn = new ObjectNode[1];
                try {
                    Unchecked.ThrowingRunnable runnable = ()->singleReturn[0] = processOneRecord(stagingAreaService, stagingAreaId, databaseRecordId, version, persist,
                            configSet.getProcessingActions());
                    log.trace("about to call runnable to call processOneRecord");
                    adminService.runAs(auth, runnable);
                    log.trace("got back singleReturn {}", singleReturn[0].toPrettyString());
                } catch (Exception e) {
                    log.error("error during import processing: ", e);
                    singleReturn[0] = JsonNodeFactory.instance.objectNode();
                    singleReturn[0].put("id", stagingAreaId);
                    singleReturn[0].put("status", "INTERNAL_SERVER_ERROR");
                    singleReturn[0].put("message", "error: " + e.getMessage());
                }
                int finalishRecord = r+1;
                TransactionTemplate transactionTemplateUpdateCount = new TransactionTemplate(transactionManager);
                transactionTemplateUpdateCount.executeWithoutResult(j->jobRepository.updateCompletedRecordCount(job.getId(),finalishRecord));
                jobRepository.updateCompletedRecordCount(job.getId(),r+1);
                returnNodes.add(singleReturn[0]);
                log.trace("appending node to returnNodes");
            }
            returnFromInnerLambda[0] =returnNodes;
            //now synchronize all records -- tidy up relationships and other entity-entity links
            if(needToSave(configSet.getProcessingActions())){
                log.trace("going to synchronize entities");
                returnNodes.forEach(n->{
                    if( n instanceof ObjectNode){
                        if(n.get("status").textValue().equalsIgnoreCase("OK")){
                            if(n.hasNonNull("PersistedEntityId")) {
                                log.trace("found PersistedEntityId");
                                String entityId = n.get("PersistedEntityId").asText();
                                Unchecked.ThrowingRunnable runnable2 = ()-> stagingAreaService.synchronizeRecord(entityId, entityClass.getName(), contextName);
                                log.trace("About to call runnable2 to call synchronizeRecord");
                                adminService.runAs(auth, runnable2);
                            } else {
                                log.warn("entity id not found!!");
                            }
                        }
                    }
                });
            }
            job.setStatusMessage("Processing completed");
            log.trace("finished processing in handleActionsAsync");
            TransactionTemplate transactionTemplate2 = new TransactionTemplate(transactionManager);
            transactionTemplate2.executeWithoutResult(t->{
                ImportProcessingJob updatedJob = jobRepository.findById(job.getId()).get();
                log.trace("going to save job");
                updatedJob.setJobStatus("completed");
                updatedJob.setStatusMessage("Processing completed");
                updatedJob.setResults(returnFromInnerLambda[0]);
                updatedJob.setFinishDate(DateUtil.getCurrentDate());
                jobRepository.save(updatedJob);
                log.trace("completed last save of job ({}) in handleActionsAsync", job.getId());
            });
        });
        log.trace("finished in handleActionsAsync");
        return job.toNode();
    }

    public JsonNode getJobAsNode(String jobId, boolean includeJobData) {
        log.trace("in getJobAsNode, jobId: {}", jobId);
        Optional<ImportProcessingJob> job = jobRepository.findById(UUID.fromString(jobId));
        if( job.isPresent()){
            return job.get().toNode(includeJobData);
        }
        ObjectNode returnNode = JsonNodeFactory.instance.objectNode();
        returnNode.put("message", "No processing job found for input");
        returnNode.put("error", true);
        return returnNode;
    }

    private ObjectNode processOneRecord(StagingAreaService stagingAreaService, String stagingRecordId, String matchedEntityId,
                                        int version, String persist, List<ProcessingActionConfig> processingActions) throws Exception {
        log.trace("starting in processOneRecord");
        ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
        if(stagingRecordId==null || stagingRecordId.length()==0){
            messageNode.put("message", "blank input");
            messageNode.put("status", "BAD_REQUEST");
            log.trace("processOneRecord going to return message node");
            return messageNode;
        }
        Matcher matcher = regExGuid.matcher(stagingRecordId);
        if(!matcher.find()){
            messageNode.put("message", "input is not a valid id");
            messageNode.put("status", "BAD_REQUEST");
            log.trace("processOneRecord going to return message node");
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
                messageNode.put("status", "BAD_REQUEST");
                messageNode.put("stagingAreaId", stagingRecordId);
                log.trace("processOneRecord going to return message node");
                return messageNode;
            }
        }
        log.trace("objectJson: {}", objectJson);
        if (objectJson == null || objectJson.length() == 0) {
            messageNode.put("message", String.format("Error retrieving staging area object of type %s with ID %s",
                    objectClass, stagingRecordId));
            messageNode.put("status", "BAD_REQUEST");
            messageNode.put("stagingAreaId", stagingRecordId);
            log.trace("processOneRecord going to return message node");
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
            messageNode.put("status", "BAD_REQUEST");
            messageNode.put("stagingAreaId", stagingRecordId);
            log.trace("processOneRecord going to return message node");
            return messageNode;
        }

        StringBuilder whatHappened = new StringBuilder();
        whatHappened.append("Processing record: ");

        ImportMetadata.RecordImportStatus recordImportStatus = ImportMetadata.RecordImportStatus.staged;
        boolean savingNewItem = false;
        for (ProcessingActionConfig configItem : processingActions) {
            ProcessingAction action =gsrsImportAdapterFactoryFactory.getMatchingProcessingAction(this.contextName, configItem.getProcessingActionName());
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
        boolean updateMetadataState = true;
        log.trace(whatHappened.toString());
        if (persist != null && persist.equalsIgnoreCase("TRUE") && isSaveNecessary(recordImportStatus)) {
            //check whether we have something to save... otherwise, skip to the next step in the processing
            if(currentObject!=null ) {
                GsrsEntityService.ProcessResult<T> result = stagingAreaService.saveEntity(objectClass, currentObject, savingNewItem);
                if (!result.isSaved()) {
                    log.error("Error! Saved object is null");
                    String errorMessage;
                    if(result.getThrowable()!=null ){
                        errorMessage=result.getThrowable().getMessage();
                    } else if( result.getValidationResponse().hasError()) {
                        errorMessage = result.getValidationResponse().getValidationMessages().stream()
                                .filter(m->m.getMessageType()== ValidationMessage.MESSAGE_TYPE.ERROR)
                                .map(m->m.getMessage())
                                .collect(Collectors.joining("|"));
                    } else {
                        errorMessage= "Unknown error!";
                    }
                    updateMetadataState=false;
                    messageNode.put("stagingAreaId", stagingRecordId);
                    messageNode.put("message","Object failed to save");
                    messageNode.put("error", errorMessage);
                    messageNode.put("status","INTERNAL_SERVER_ERROR");
                    return messageNode;
                }
                currentObject = result.getEntity();
                messageNode.put("PersistedEntityId", result.getEntityId().toString());
                log.trace("saved new or updated entity");
            }

        } else {
            log.trace("skipped saving/processing");
        }

        if(updateMetadataState) {
            stagingAreaService.updateRecordImportStatus(recordId, recordImportStatus);
            ImportMetadata metadata = stagingAreaService.getImportMetaData(recordId.toString(), 0);
            EntityUtils.EntityWrapper<ImportMetadata> wrappedObject = EntityUtils.EntityWrapper.of(metadata);
            ImportMetadata refetchedMetadata = (ImportMetadata) EntityFetcher.of(wrappedObject.getKey()).getIfPossible().get();
            refetchedMetadata.setImportStatus(recordImportStatus);
            EntityUtils.EntityWrapper<ImportMetadata> reWrappedObject = EntityUtils.EntityWrapper.of(refetchedMetadata);
            UUID indexingEventId = UUID.randomUUID();
            ImportMetadataReindexer.indexOneItem(indexingEventId, eventPublisher::publishEvent, EntityUtils.Key.of(reWrappedObject),
                    reWrappedObject);
            log.trace("reindexing importmetadata object");
        }

        if( currentObject!=null) {
            log.trace("currentObject not null");
            //messageNode.put("object", mapper.writeValueAsString(currentObject));
            messageNode.put("message", "Import record processed successfully");
            messageNode.put("stagingAreaId", stagingRecordId);
            messageNode.put("status", "OK");
            log.trace("about to return message node {}", messageNode.toPrettyString());
            return messageNode;
        }else{
            messageNode.put("message", "Import record processed successfully");
            messageNode.put("stagingAreaId", stagingRecordId);
            messageNode.put("status", "OK");
            log.trace("about to return message node {}", messageNode.toPrettyString());
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
        ImportAdapterFactory<T> adaptFac = getImportAdapterFactory(task)
                .orElse(null);
        if (adaptFac == null) {
            throw new IOException("Cannot predict settings with unknown import adapter:\"" + task.getAdapter() + "\"");
        }
        log.trace("in fetchAdapterFactory, adaptFac: {}", adaptFac);
        log.trace("in fetchAdapterFactory, adaptFac: {}", adaptFac.getClass().getName());
        //log.trace("in fetchAdapterFactory, staging area service: {}", adaptFac.getStagingAreaService().getName());
        adaptFac.setFileName(task.getFilename());
        log.trace("passed file name {} to adapter factory", task.getFilename());
        return adaptFac;
    }


    public List<ImportAdapterFactory<T>> getImportAdapters() {
        assert gsrsImportAdapterFactoryFactory != null;
        log.trace("starting in getImportAdapters.  gsrsImportAdapterFactoryFactory: {}, this.contextName: {}, entityClass: {}",
                gsrsImportAdapterFactoryFactory, this.contextName, entityClass != null ? entityClass.getName() : "null!");
        gsrsImportAdapterFactoryFactory.newFactory(this.contextName, entityClass);
        return importAdapterFactories.get();
    }

    public Optional<ImportAdapterFactory<T>> getImportAdapterFactory(AbstractImportSupportingGsrsEntityController.ImportTaskMetaData<T> task) {
        log.trace("In getImportAdapterFactory, looking for adapter with name '{}' among {}", task.getAdapter(), getImportAdapters().size());
        if (getImportAdapters() != null) {
            getImportAdapters().forEach(a -> log.trace("adapter with name: '{}', key: '{}'", a.getAdapterName(), a.getAdapterKey()));
        }
        Optional<ImportAdapterFactory<T>> adapterFactory = getImportAdapters().stream().filter(n -> task.getAdapter().equals(n.getAdapterName())).findFirst();
        if (!adapterFactory.isPresent()) {
            log.trace("searching for adapter by name failed; using key");
            adapterFactory = getImportAdapters().stream().filter(n -> task.getAdapter().equals(n.getAdapterKey())).findFirst();
            log.trace("success? {}", adapterFactory.isPresent());
        }
        return adapterFactory;
    }


    public Stream<T> generateObjects(AbstractImportSupportingGsrsEntityController.ImportTaskMetaData<T> task, Map<String, String> settingsMap) throws Exception {
        log.trace("starting in generateObjects. task: " + task.toString());
        log.trace("using encoding {}, looking for payload with ID {}", task.getFileEncoding(), task.getPayloadID());
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode settingsNode = mapper.convertValue(settingsMap, ObjectNode.class);
        if (!settingsNode.hasNonNull("Encoding")) {
            settingsNode.put("Encoding", task.getFileEncoding());
        }
        ImportAdapterFactory<T> factory = fetchAdapterFactory(task);
        ImportAdapter<T> adapter = factory.createAdapter(task.getAdapterSettings());
        Optional<InputStream> streamHolder = payloadService.getPayloadAsInputStream(task.getPayloadID());
        if (streamHolder.isPresent()) {
            InputStream stream = streamHolder.get();
            return adapter.parse(stream, settingsNode, task.getAdapterSchema());
        }
        return Stream.empty();
    }

    public JsonNode handleObjectCreation(AbstractImportSupportingGsrsEntityController.ImportTaskMetaData task,
                                       Map<String, String> queryParameters) throws Exception {
        Stream<T> objectStream = generateObjects(task, queryParameters);

        ObjectMapper mapper = new ObjectMapper();
        AtomicBoolean objectProcessingOK = new AtomicBoolean(true);
        AtomicInteger recordCount = new AtomicInteger(0);
        List<Integer> errorRecords = new ArrayList<>();
        List<String> importDataRecordIds = new ArrayList<>();
        ArrayNode previewNode = JsonNodeFactory.instance.arrayNode();
        Principal importingUser = (GsrsSecurityUtils.getCurrentUsername()!=null && GsrsSecurityUtils.getCurrentUsername().isPresent())
                ? principalRepository.findDistinctByUsernameIgnoreCase(GsrsSecurityUtils.getCurrentUsername().get())
                : null;

        objectStream.forEach(object -> {
            recordCount.incrementAndGet();
            log.trace("going to call saveStagingAreaRecord with data of type {}", object.getClass().getName());
            log.trace(object.toString());
            try {
                String newRecordId =saveStagingAreaRecord(mapper.writeValueAsString(object), task, importingUser);
                importDataRecordIds.add(newRecordId);
                    /*if (recordCount.get() < limit) {
                        if (object instanceof Supplier) {
                            log.trace("going to invoke supplier on object");
                            object = (T) ((Supplier) object).get();
                        }
                        //previewNode.add(mapper.writeValueAsString(object));
                        ObjectNode singleRecord = JsonNodeFactory.instance.objectNode();
                        JsonNode dataAsNode = mapper.readTree(mapper.writeValueAsString(object));
                        singleRecord.set("data", dataAsNode);
                        MatchedRecordSummary matchSummary = service.findMatchesForJson(itmd.entityType, mapper.writeValueAsString(object),
                                newRecordId);
                        JsonNode matchesAsNode = mapper.readTree(mapper.writeValueAsString(matchSummary));
                        singleRecord.set("matches", matchesAsNode);
                        previewNode.add(singleRecord);
                    }*/
            } catch (JsonProcessingException e) {
                objectProcessingOK.set(false);
                errorRecords.add(recordCount.get());
                log.error("Error processing staging area record", e);
            }
        });

        ObjectNode returnNode = JsonNodeFactory.instance.objectNode();
        returnNode.put("completeSuccess", objectProcessingOK.get());
        ArrayNode recordIdListNode = JsonNodeFactory.instance.arrayNode();
        importDataRecordIds.forEach(recordIdListNode::add);
        returnNode.set("stagingAreaRecordIds", recordIdListNode);
        ArrayNode problemRecords = JsonNodeFactory.instance.arrayNode();
        errorRecords.forEach(problemRecords::add);
        returnNode.set("recordsWithProcessingErrors", problemRecords);
        returnNode.put("fileName", task.getFilename());
        returnNode.put("adapter", task.getAdapter());
        long limit = Long.parseLong(queryParameters.getOrDefault("limit", "10"));
        returnNode.put("limit", limit);
        log.trace("Attached limit to returnNode");
        returnNode.set("dataPreview", previewNode);
        return returnNode;
    }

    public JsonNode handleObjectCreationAsync(AbstractImportSupportingGsrsEntityController.ImportTaskMetaData task,
                                         Map<String, String> queryParameters) {

        log.trace("starting in handleObjectCreationAsync");
        log.trace("task: {}", task.toString());
        ObjectMapper mapper = new ObjectMapper();
        AtomicBoolean objectProcessingOK = new AtomicBoolean(true);
        AtomicInteger recordCount = new AtomicInteger(0);
        List<Integer> errorRecords = new ArrayList<>();
        List<String> importDataRecordIds = new ArrayList<>();
        ImportProcessingJob job = new ImportProcessingJob();
        job.setId(UUID.randomUUID());
        job.setStartDate(DateUtil.getCurrentDate());
        job.setJobData(task.toString());
        job.setJobStatus("starting");
        job.setCategory("Push to Staging Area");
        job.setStatusMessage("Processing started");
        if( task.getAdapterSettings().hasNonNull("RecordCount")) {
            job.setTotalRecords((task.getAdapterSettings()).get("RecordCount").asInt());
        }

        job.setCompletedRecordCount(0);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(r-> jobRepository.saveAndFlush(job));

        if(queryParameters.containsKey("skipValidation") || queryParameters.containsKey("skipIndexing") || queryParameters.containsKey("skipMatching")) {
            log.trace("keys : {}", String.join("||", queryParameters.keySet()));
            ObjectNode rareSettings = JsonNodeFactory.instance.objectNode();
            if(queryParameters.containsKey("skipValidation") && queryParameters.get("skipValidation").length()>0) {
                boolean value = queryParameters.get("skipValidation").equalsIgnoreCase("true");
                rareSettings.put("skipValidation", value);
                log.trace("skipValidation: {}", value);
            }
            if(queryParameters.containsKey("skipIndexing") && queryParameters.get("skipIndexing").length()>0) {
                boolean value = queryParameters.get("skipIndexing").equalsIgnoreCase("true");
                rareSettings.put("skipIndexing", value);
                log.trace("skipIndexing: {}", value);
            }
            if(queryParameters.containsKey("skipMatching") && queryParameters.get("skipMatching").length()>0) {
                boolean value = queryParameters.get("skipMatching").equalsIgnoreCase("true");
                rareSettings.put("skipMatching", value);
                log.trace("skipMatching: {}", value);
            }
            log.trace("creating rarelyUsedSettings node");
            ((ObjectNode)task.getAdapterSettings()).set("rarelyUsedSettings", rareSettings);
        }
        log.trace("saved init job");
        Principal importingUser = (GsrsSecurityUtils.getCurrentUsername()!=null && GsrsSecurityUtils.getCurrentUsername().isPresent())
            ? principalRepository.findDistinctByUsernameIgnoreCase(GsrsSecurityUtils.getCurrentUsername().get())
            : null;
        executor.execute(()-> {
            log.trace("starting in handleObjectCreationAsync execute lambda");
            ArrayNode previewNode = JsonNodeFactory.instance.arrayNode();
            Stream<T> objectStream;
            try {
                objectStream = generateObjects(task, queryParameters);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            objectStream.forEach(object -> {
                recordCount.incrementAndGet();
                log.trace("handleObjectCreationAsync going to call saveStagingAreaRecord with data of type {}", object.getClass().getName());
                log.trace(object.toString());
                try {
                    String newRecordId = saveStagingAreaRecord(mapper.writeValueAsString(object), task, importingUser);
                    importDataRecordIds.add(newRecordId);
                } catch (JsonProcessingException e) {
                    objectProcessingOK.set(false);
                    errorRecords.add(recordCount.get());
                    log.error("Error processing staging area record", e);
                }
                TransactionTemplate transactionTemplateUpDateCount = new TransactionTemplate(transactionManager);
                transactionTemplateUpDateCount.executeWithoutResult(j->jobRepository.updateCompletedRecordCount(job.getId(), recordCount.get()));
            });

            ObjectNode returnNode = JsonNodeFactory.instance.objectNode();
            returnNode.put("completeSuccess", objectProcessingOK.get());
            ArrayNode recordIdListNode = JsonNodeFactory.instance.arrayNode();
            importDataRecordIds.forEach(recordIdListNode::add);
            returnNode.set("stagingAreaRecordIds", recordIdListNode);
            ArrayNode problemRecords = JsonNodeFactory.instance.arrayNode();
            errorRecords.forEach(problemRecords::add);
            returnNode.set("recordsWithProcessingErrors", problemRecords);
            returnNode.put("fileName", task.getFilename());
            returnNode.put("adapter", task.getAdapter());
            long limit = Long.parseLong(queryParameters.getOrDefault("limit", "10"));
            returnNode.put("limit", limit);
            log.trace("handleObjectCreationAsync limit to returnNode");
            returnNode.set("dataPreview", previewNode);
            ArrayNode overallResult= JsonNodeFactory.instance.arrayNode();
            overallResult.add(returnNode);

            TransactionTemplate transactionTemplate2 = new TransactionTemplate(transactionManager);
            transactionTemplate2.executeWithoutResult(t->{
                ImportProcessingJob updatedJob = jobRepository.findById(job.getId()).get();
                log.trace("going to save job");
                updatedJob.setJobStatus("completed");
                updatedJob.setStatusMessage("Processing completed");
                updatedJob.setResults(overallResult);
                updatedJob.setFinishDate(DateUtil.getCurrentDate());
                updatedJob.setTotalRecords(recordCount.get());
                jobRepository.save(updatedJob);
                log.trace("completed last save of job ({}) in handleObjectCreationAsync", job.getId());
            });

        });
        return job.toNode();
    }

    public String saveStagingAreaRecord(String json, AbstractImportSupportingGsrsEntityController.ImportTaskMetaData importTaskMetaData, Principal creatingUser) {
        log.trace("in saveStagingAreaRecord,importTaskMetaData.getEntityType(): {}, file name: {}, adapter",
                importTaskMetaData.getEntityType(), importTaskMetaData.getFilename(), importTaskMetaData.getAdapter());
        ImportRecordParameters.ImportRecordParametersBuilder builder=
         ImportRecordParameters.builder()
                .jsonData(json)
                .entityClassName(importTaskMetaData.getEntityType())
                .formatType(importTaskMetaData.getMimeType())
                .source(importTaskMetaData.getFilename())
                .adapterName(importTaskMetaData.getAdapter())
                 .importingUser(creatingUser);
        if(importTaskMetaData.getAdapterSettings().hasNonNull("rarelyUsedSettings")) {
            JsonNode rareSettings = importTaskMetaData.getAdapterSettings().get("rarelyUsedSettings");
            log.trace("processing rarelyUsedSettings {}", rareSettings.toPrettyString());
            ObjectNode parent = JsonNodeFactory.instance.objectNode();
            parent.set("rarelyUsedSettings", rareSettings);
            builder.settings(parent);
        }
        ImportRecordParameters parameters =builder.build();
        return stagingAreaService.createRecord(parameters);
    }

    public List<String> getOptionsForAction(String actionName){
        ProcessingAction action =gsrsImportAdapterFactoryFactory.getMatchingProcessingAction(this.contextName, actionName);
        if( action!=null) {
            return action.getOptions();
        }
        return Collections.EMPTY_LIST;
    }

    public JsonNode getSchemaForAction(String actionName){
        ProcessingAction action =gsrsImportAdapterFactoryFactory.getMatchingProcessingAction(this.contextName, actionName);
        if( action!=null) {
            return action.getAvailableSettingsSchema();
        }
        return JsonNodeFactory.instance.objectNode();
    }

    private boolean needToSave(List<ProcessingActionConfig> actionConfigs){
        return actionConfigs.stream().anyMatch(c->c.getProcessingActionName().toUpperCase().contains("CREATE"));
    }

    public JsonNode handleDeletion(String[] ids, boolean reindex){
        log.trace("in handleDeletion");
        ArrayNode deleted = JsonNodeFactory.instance.arrayNode();
        int version = 0;//possible to retrieve from parameters. For now, assume latest
        Arrays.stream(ids)
                .filter(id->id!=null)
                .map(id->id.replace("\"", "")
                        .replace(",","")
                        .replaceAll(" ","")
                        .replace("\r","")
                        .replace("\n","")
                        .replace("\t",""))
                .filter(id->id.length()>0)
                .forEach(id->{
                    if(reindex){
                        try {
                            ImportMetadata object= stagingAreaService.getImportMetaData(id, version);
                            if( object !=null) {
                                textIndexerEntityListener.deleteEntity(new IndexRemoveEntityEvent(EntityUtils.EntityWrapper.of(object)));
                                log.trace("submitted index deletion");
                            } else{
                                log.warn("Error retrieving ImportMetadata with ID {}", id);
                            }
                        } catch (Exception e) {
                            log.error("Error during removal from index: ", e);

                        }
                    }
                    stagingAreaService.deleteRecord(id, version);
                    log.trace("deleted record with ID {}", id);
                    deleted.add(id);
                });

        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.set("deleted records", deleted);
        return response;
    }
}
