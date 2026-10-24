package gsrs.imports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ImportUtilities<T> {

    private static final Logger IMPORT_PERFORMANCE_LOG =
            LoggerFactory.getLogger("gsrs.import.performance");

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

    @PersistenceContext
    private EntityManager entityManager;

    private String contextName;

    private Class<T> entityClass;

    private static final Pattern regExGuid= Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    private static final JsonMapper mapper = JsonMapper.builderWithJackson2Defaults()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    StagingAreaService stagingAreaService;

    @Value("${gsrs.import.progress-update-interval:100}")
    private int progressUpdateInterval = 100;

    @Value("${gsrs.import.chunk-size:50}")
    private int importChunkSize = 50;

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
            } catch (Exception e) {
                log.error("Error in getAllImportTasks", e);
            }
        });
        return allImportConfigs;
    }

    public static String removeMetadataFromDomainObjectJson(String domainObjectJson) {
        try {
            JsonNode objectAsNode = mapper.readTree(domainObjectJson);
            if (objectAsNode.hasNonNull("_metadata") && objectAsNode.isObject()) {
                ((ObjectNode) objectAsNode).remove("_metadata");
            }
            if (objectAsNode.hasNonNull("_matches") && objectAsNode.isObject()) {
                ((ObjectNode) objectAsNode).remove("_matches");
            }
            return objectAsNode.toString();
        } catch (Exception e) {
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
            } catch (Exception e) {
                log.error("Error", e);
            }
            assert config != null;
            return config.getExporterKey() != null && config.getExporterKey().equalsIgnoreCase(importerId);
        });
    }

    public ObjectNode handleAction(StagingAreaService stagingAreaService, String matchedEntityId, String stagingRecordId,
                                        int version, String persist, String processingJson, String contextName ) throws Exception {
        assert stagingAreaService != null;
        ProcessingActionConfigSet configSet = mapper.readValue(processingJson, ProcessingActionConfigSet.class);
        return processOneRecord(stagingAreaService, stagingRecordId, matchedEntityId, version, persist, configSet.getProcessingActions());
    }

    public List<ObjectNode> handleActions(StagingAreaService stagingAreaService,
                                   int version, String persist, String processingJson) throws Exception {
        assert stagingAreaService != null;
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
                } catch (Throwable e) {
                    log.error("error during import processing: ", e);
                    singleReturn[0] = JsonNodeFactory.instance.objectNode();
                    singleReturn[0].put("id", stagingAreaId);
                    setActionStatus(singleReturn[0], HttpStatus.INTERNAL_SERVER_ERROR);
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
                                try {
									adminService.runAs(auth, runnable2);
								} catch (Throwable e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
                            } else {
                                log.warn("entity id not found!!");
                            }
                        }
                    }
                });
            }
            log.trace("finished processing in handleActionsAsync");
            boolean processingFailed = hasFailedResults(returnNodes);
            String finalJobStatus = processingFailed ? "failed" : "completed";
            String finalStatusMessage = processingFailed
                    ? "Processing completed with errors"
                    : "Processing completed";
            TransactionTemplate transactionTemplate2 = new TransactionTemplate(transactionManager);
            transactionTemplate2.executeWithoutResult(t->{
                ImportProcessingJob updatedJob = jobRepository.findById(job.getId()).get();
                log.trace("going to save job");
                updatedJob.setJobStatus(finalJobStatus);
                updatedJob.setStatusMessage(finalStatusMessage);
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
            ObjectNode jobNode = job.get().toNode(includeJobData);
            if ("failed".equalsIgnoreCase(job.get().getJobStatus())) {
                jobNode.put("error", true);
            }
            return jobNode;
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
            setActionStatus(messageNode, HttpStatus.BAD_REQUEST);
            log.trace("processOneRecord going to return message node");
            return messageNode;
        }
        Matcher matcher = regExGuid.matcher(stagingRecordId);
        if(!matcher.find()){
            messageNode.put("message", "input is not a valid id");
            setActionStatus(messageNode, HttpStatus.BAD_REQUEST);
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
                setActionStatus(messageNode, HttpStatus.BAD_REQUEST);
                messageNode.put("stagingAreaId", stagingRecordId);
                log.trace("processOneRecord going to return message node");
                return messageNode;
            }
        }
        log.trace("objectJson: {}", objectJson);
        if (objectJson == null || objectJson.length() == 0) {
            messageNode.put("message", String.format("Error retrieving staging area object of type %s with ID %s",
                    objectClass, stagingRecordId));
            setActionStatus(messageNode, HttpStatus.BAD_REQUEST);
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
            setActionStatus(messageNode, HttpStatus.BAD_REQUEST);
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
                if (result == null || !result.isSaved()) {
                    log.error("Error! Entity save failed");
                    updateMetadataState=false;
                    return createSaveFailureResponse(stagingRecordId, result);
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
            messageNode.put("message", "Import record processed successfully");
            messageNode.put("stagingAreaId", stagingRecordId);
            setActionStatus(messageNode, HttpStatus.OK);
            log.trace("about to return message node {}", messageNode.toPrettyString());
            return messageNode;
        }else{
            messageNode.put("message", "Import record processed successfully");
            messageNode.put("stagingAreaId", stagingRecordId);
            setActionStatus(messageNode, HttpStatus.OK);
            log.trace("about to return message node {}", messageNode.toPrettyString());
            return messageNode;
        }
    }

    static ObjectNode setActionStatus(ObjectNode response, HttpStatus status) {
        response.put("status", status.name());
        response.put("httpStatus", status.value());
        return response;
    }

    static boolean hasFailedResults(ArrayNode results) {
        if (results == null) {
            return true;
        }
        for (JsonNode result : results) {
            if (!result.hasNonNull("status")
                    || !HttpStatus.OK.name().equalsIgnoreCase(result.get("status").asText())) {
                return true;
            }
        }
        return false;
    }

    static ObjectNode createSaveFailureResponse(String stagingRecordId,
                                                GsrsEntityService.ProcessResult<?> result) {
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.put("stagingAreaId", stagingRecordId);
        response.put("message", "Object failed to save");
        response.put("error", getSaveFailureMessage(result));
        return setActionStatus(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static String getSaveFailureMessage(GsrsEntityService.ProcessResult<?> result) {
        if (result == null) {
            return "Save operation returned no result";
        }
        if (result.getThrowable() != null) {
            String throwableMessage = result.getThrowable().getMessage();
            return throwableMessage == null || throwableMessage.isBlank()
                    ? result.getThrowable().toString()
                    : throwableMessage;
        }
        if (result.getValidationResponse() != null && result.getValidationResponse().hasError()) {
            String validationErrors = result.getValidationResponse().getValidationMessages().stream()
                    .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR)
                    .map(ValidationMessage::getMessage)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("|"));
            if (!validationErrors.isBlank()) {
                return validationErrors;
            }
        }
        return "Unknown error";
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
        try( Stream<T> objectStream = generateObjects(task, queryParameters) ) {

            AtomicBoolean objectProcessingOK = new AtomicBoolean(true);
            AtomicInteger recordCount = new AtomicInteger(0);
            List<Integer> errorRecords = new ArrayList<>();
            List<String> importDataRecordIds = new ArrayList<>();
            ArrayNode previewNode = JsonNodeFactory.instance.arrayNode();
            Principal importingUser = (GsrsSecurityUtils.getCurrentUsername() != null && GsrsSecurityUtils.getCurrentUsername().isPresent())
                    ? principalRepository.findDistinctByUsernameIgnoreCase(GsrsSecurityUtils.getCurrentUsername().get())
                    : null;

            objectStream.forEach(object -> {
                recordCount.incrementAndGet();
                log.trace("going to call saveStagingAreaRecord with data of type {}", object.getClass().getName());
                log.trace(object.toString());
                try {
                    String newRecordId = saveStagingAreaRecord(mapper.writeValueAsString(object), task, importingUser);
                    importDataRecordIds.add(newRecordId);
                } catch (Exception e) {
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
    }

    public JsonNode handleObjectCreationAsync(AbstractImportSupportingGsrsEntityController.ImportTaskMetaData task,
                                         Map<String, String> queryParameters) {

        log.trace("starting in handleObjectCreationAsync");
        log.trace("task: {}", task.toString());
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
        executor.execute(() -> processObjectCreationJob(job, task, queryParameters, importingUser));

        return job.toNode();
    }

    private void processObjectCreationJob(
            ImportProcessingJob job,
            AbstractImportSupportingGsrsEntityController.ImportTaskMetaData task,
            Map<String, String> queryParameters,
            Principal importingUser) {

        log.trace("starting asynchronous staging import");
        AtomicBoolean objectProcessingOK = new AtomicBoolean(true);
        AtomicInteger recordCount = new AtomicInteger(0);
        List<Integer> errorRecords = new ArrayList<>();
        List<String> importDataRecordIds = new ArrayList<>();
        ArrayNode previewNode = JsonNodeFactory.instance.arrayNode();
        ImportTimings timings = new ImportTimings();
        int completedRecordCount = 0;
        int lastProgressUpdate = 0;
        String finalStatus = "completed";
        String statusMessage = "Processing completed";

        try (Stream<T> objectStream = generateObjects(task, queryParameters)) {
            int chunkSize = Math.max(1, importChunkSize);
            List<PendingStagingRecord> chunk = new ArrayList<>(chunkSize);
            Iterator<T> iterator = objectStream.iterator();

            while (iterator.hasNext()) {
                T object = iterator.next();
                int currentRecord = recordCount.incrementAndGet();
                log.trace("serializing staging record {} of type {}", currentRecord,
                        object == null ? "null" : object.getClass().getName());
                long serializationStart = System.nanoTime();
                try {
                    chunk.add(new PendingStagingRecord(
                            currentRecord,
                            mapper.writeValueAsString(object)));
                } catch (Exception e) {
                    objectProcessingOK.set(false);
                    errorRecords.add(currentRecord);
                    log.error("Error serializing staging area record {}", currentRecord, e);
                } finally {
                    timings.recordSerialization(System.nanoTime() - serializationStart);
                }

                if (isChunkBoundary(currentRecord, chunkSize)) {
                    persistChunkWithFallback(
                            chunk, task, importingUser, job.getId(), importDataRecordIds,
                            errorRecords, timings);
                    chunk.clear();
                    completedRecordCount = currentRecord;
                    lastProgressUpdate = updateProgressIfNeeded(
                            job.getId(), completedRecordCount, lastProgressUpdate);
                    timings.logIfNeeded(completedRecordCount, progressUpdateInterval);
                }
            }

            if (completedRecordCount < recordCount.get()) {
                persistChunkWithFallback(
                        chunk, task, importingUser, job.getId(), importDataRecordIds,
                        errorRecords, timings);
                completedRecordCount = recordCount.get();
                updateProgressIfNeeded(job.getId(), completedRecordCount, lastProgressUpdate);
            }
            if (!errorRecords.isEmpty()) {
                objectProcessingOK.set(false);
                finalStatus = "completed-with-errors";
                statusMessage = "Processing completed with errors";
            }
        } catch (Exception e) {
            objectProcessingOK.set(false);
            finalStatus = "failed";
            statusMessage = "Processing failed: " + safeExceptionMessage(e);
            log.error("Fatal error handling object stream for job {}", job.getId(), e);
        }

        long indexingDrainStart = System.nanoTime();
        try {
            stagingAreaService.awaitIndexingCompletion(job.getId());
        } catch (RuntimeException indexingFailure) {
            objectProcessingOK.set(false);
            finalStatus = "failed";
            statusMessage = "Processing failed: " + safeExceptionMessage(indexingFailure);
            log.error("Indexing failed for staging import job {}", job.getId(), indexingFailure);
        } finally {
            timings.recordIndexingDrain(System.nanoTime() - indexingDrainStart);
            timings.logFinal(recordCount.get());
        }

        ObjectNode returnNode = buildObjectCreationResult(
                task,
                queryParameters,
                objectProcessingOK.get(),
                importDataRecordIds,
                errorRecords,
                previewNode);
        ArrayNode overallResult = JsonNodeFactory.instance.arrayNode();
        overallResult.add(returnNode);
        completeObjectCreationJob(
                job.getId(),
                finalStatus,
                statusMessage,
                overallResult,
                job.getTotalRecords() > 0 ? job.getTotalRecords() : recordCount.get(),
                completedRecordCount);
    }

    private void persistChunkWithFallback(
            List<PendingStagingRecord> chunk,
            AbstractImportSupportingGsrsEntityController.ImportTaskMetaData task,
            Principal importingUser,
            UUID indexingBatchId,
            List<String> importDataRecordIds,
            List<Integer> errorRecords,
            ImportTimings timings) {

        if (chunk.isEmpty()) {
            return;
        }

        long createRecordsStart = System.nanoTime();
        try {
            importDataRecordIds.addAll(
                    persistChunk(chunk, task, importingUser, indexingBatchId));
        } catch (RuntimeException chunkFailure) {
            log.warn("Staging chunk failed; retrying {} records individually", chunk.size(), chunkFailure);
            for (PendingStagingRecord record : chunk) {
                try {
                    importDataRecordIds.add(
                            persistSingleRecord(record, task, importingUser, indexingBatchId));
                } catch (RuntimeException recordFailure) {
                    errorRecords.add(record.recordNumber());
                    log.error("Error processing staging area record {}", record.recordNumber(), recordFailure);
                }
            }
        } finally {
            timings.recordCreateRecords(System.nanoTime() - createRecordsStart, chunk.size());
        }
    }

    private List<String> persistChunk(
            List<PendingStagingRecord> chunk,
            AbstractImportSupportingGsrsEntityController.ImportTaskMetaData task,
            Principal importingUser,
            UUID indexingBatchId) {

        TransactionTemplate chunkTransaction = new TransactionTemplate(transactionManager);
        List<String> recordIds = chunkTransaction.execute(status -> {
            List<String> ids = new ArrayList<>(chunk.size());
            for (PendingStagingRecord record : chunk) {
                ids.add(saveStagingAreaRecord(
                        record.json(), task, importingUser, indexingBatchId));
            }
            flushAndClearPersistenceContext();
            return ids;
        });
        return recordIds == null ? Collections.emptyList() : recordIds;
    }

    private String persistSingleRecord(
            PendingStagingRecord record,
            AbstractImportSupportingGsrsEntityController.ImportTaskMetaData task,
            Principal importingUser,
            UUID indexingBatchId) {

        TransactionTemplate recordTransaction = new TransactionTemplate(transactionManager);
        recordTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return recordTransaction.execute(status -> {
            String recordId = saveStagingAreaRecord(
                    record.json(), task, importingUser, indexingBatchId);
            flushAndClearPersistenceContext();
            return recordId;
        });
    }

    private void flushAndClearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    private int updateProgressIfNeeded(UUID jobId, int completedCount, int lastUpdatedCount) {
        if (!shouldUpdateProgress(completedCount, lastUpdatedCount, progressUpdateInterval)) {
            return lastUpdatedCount;
        }
        TransactionTemplate progressTransaction = new TransactionTemplate(transactionManager);
        progressTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        progressTransaction.executeWithoutResult(status ->
                jobRepository.updateCompletedRecordCount(jobId, completedCount));
        return completedCount;
    }

    static boolean shouldUpdateProgress(int completedCount, int lastUpdatedCount, int interval) {
        return interval > 0 && completedCount > 0 && completedCount - lastUpdatedCount >= interval;
    }

    static boolean isChunkBoundary(int recordNumber, int chunkSize) {
        return recordNumber > 0 && chunkSize > 0 && recordNumber % chunkSize == 0;
    }

    private ObjectNode buildObjectCreationResult(
            AbstractImportSupportingGsrsEntityController.ImportTaskMetaData task,
            Map<String, String> queryParameters,
            boolean completeSuccess,
            List<String> importDataRecordIds,
            List<Integer> errorRecords,
            ArrayNode previewNode) {

        ObjectNode returnNode = JsonNodeFactory.instance.objectNode();
        returnNode.put("completeSuccess", completeSuccess);
        ArrayNode recordIdListNode = JsonNodeFactory.instance.arrayNode();
        importDataRecordIds.forEach(recordIdListNode::add);
        returnNode.set("stagingAreaRecordIds", recordIdListNode);
        ArrayNode problemRecords = JsonNodeFactory.instance.arrayNode();
        errorRecords.forEach(problemRecords::add);
        returnNode.set("recordsWithProcessingErrors", problemRecords);
        returnNode.put("fileName", task.getFilename());
        returnNode.put("adapter", task.getAdapter());
        returnNode.put("limit", parseLongOrDefault(queryParameters.get("limit"), 10L));
        returnNode.set("dataPreview", previewNode);
        return returnNode;
    }

    private void completeObjectCreationJob(
            UUID jobId,
            String finalStatus,
            String statusMessage,
            ArrayNode results,
            int totalRecords,
            int completedRecords) {

        TransactionTemplate completionTransaction = new TransactionTemplate(transactionManager);
        completionTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        completionTransaction.executeWithoutResult(status -> {
            ImportProcessingJob updatedJob = jobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalStateException("Import job not found: " + jobId));
            updatedJob.setJobStatus(finalStatus);
            updatedJob.setStatusMessage(statusMessage);
            updatedJob.setResults(results);
            updatedJob.setFinishDate(DateUtil.getCurrentDate());
            updatedJob.setTotalRecords(totalRecords);
            updatedJob.setCompletedRecordCount(completedRecords);
            jobRepository.save(updatedJob);
            log.trace("completed final save of staging import job {} with status {}", jobId, finalStatus);
        });
    }

    private static String safeExceptionMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }

    private static long parseLongOrDefault(String value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private record PendingStagingRecord(int recordNumber, String json) {}

    public String saveStagingAreaRecord(String json, AbstractImportSupportingGsrsEntityController.ImportTaskMetaData importTaskMetaData, Principal creatingUser) {
        return saveStagingAreaRecord(json, importTaskMetaData, creatingUser, null);
    }

    private String saveStagingAreaRecord(
            String json,
            AbstractImportSupportingGsrsEntityController.ImportTaskMetaData importTaskMetaData,
            Principal creatingUser,
            UUID indexingBatchId) {
        log.trace("in saveStagingAreaRecord,importTaskMetaData.getEntityType(): {}, file name: {}, adapter: {}",
                importTaskMetaData.getEntityType(), importTaskMetaData.getFilename(), importTaskMetaData.getAdapter());
        ImportRecordParameters.ImportRecordParametersBuilder builder=
         ImportRecordParameters.builder()
                .jsonData(json)
                .entityClassName(importTaskMetaData.getEntityType())
                .formatType(importTaskMetaData.getMimeType())
                .source(importTaskMetaData.getFilename())
                .adapterName(importTaskMetaData.getAdapter())
                .importingUser(creatingUser)
                .indexingBatchId(indexingBatchId);
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
                        removeImportMetadataFromIndex(id, version);
                    }
                    stagingAreaService.deleteRecord(id, version);
                    log.trace("deleted record with ID {}", id);
                    deleted.add(id);
                });

        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.set("deleted records", deleted);
        return response;
    }

    public void removeImportMetadataFromIndex(String metadataId, int version) {
        ImportMetadata object= stagingAreaService.getImportMetaData(metadataId, version);
        if( object !=null) {
            try {
                textIndexerEntityListener.deleteEntity(new IndexRemoveEntityEvent(EntityUtils.EntityWrapper.of(object)));
            } catch (Exception e) {
                log.error("error removing ImportMetadata object from index: ", e);
            }
            log.trace("submitted index deletion");
        } else{
            log.warn("Error retrieving ImportMetadata with ID {}", metadataId);
        }
    }

    private static final class ImportTimings {
        private final LongAdder serialization = new LongAdder();
        private final LongAdder serializationSamples = new LongAdder();
        private final LongAdder createRecordTotal = new LongAdder();
        private final LongAdder createRecordSamples = new LongAdder();
        private final LongAdder indexingDrain = new LongAdder();

        void recordSerialization(long nanos) {
            serialization.add(nanos);
            serializationSamples.increment();
        }

        void recordCreateRecords(long nanos, int recordCount) {
            createRecordTotal.add(nanos);
            createRecordSamples.add(recordCount);
        }

        void recordIndexingDrain(long nanos) {
            indexingDrain.add(nanos);
        }

        void logIfNeeded(int processedCount, int interval) {
            if (interval <= 0 || processedCount <= 0 || processedCount % interval != 0) {
                return;
            }
            if (IMPORT_PERFORMANCE_LOG.isTraceEnabled()) {
                log(processedCount);
            }
        }

        void logFinal(int processedCount) {
            if (IMPORT_PERFORMANCE_LOG.isTraceEnabled() && processedCount > 0) {
                log(processedCount);
            }
        }

        private void log(int processedCount) {
            long serializationCount = serializationSamples.sum();
            long createRecordCount = createRecordSamples.sum();
            IMPORT_PERFORMANCE_LOG.trace(
                    "Import timing: processedRecords={}, serializationSamples={}, "
                            + "serialization avg={} ms, createRecordSamples={}, "
                            + "createRecord+commit avg={} ms, indexing drain={} ms",
                    processedCount,
                    serializationCount,
                    nanosToAverageMillis(serialization.sum(), serializationCount),
                    createRecordCount,
                    nanosToAverageMillis(createRecordTotal.sum(), createRecordCount),
                    indexingDrain.sum() / 1_000_000.0);
        }

        private static double nanosToAverageMillis(
                long nanos,
                long count) {

            return count == 0 ? 0.0 : nanos / 1_000_000.0 / count;
        }
    }

}
