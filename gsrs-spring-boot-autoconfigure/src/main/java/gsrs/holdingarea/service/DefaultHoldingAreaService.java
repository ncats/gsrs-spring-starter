package gsrs.holdingarea.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.events.ReindexEntityEvent;
import gsrs.holdingarea.model.*;
import gsrs.holdingarea.repository.*;
import gsrs.indexer.IndexValueMakerFactory;
import gsrs.service.GsrsEntityService;
import gsrs.springUtils.AutowireHelper;
import gsrs.validator.GsrsValidatorFactory;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.GinasCommonData;
import ix.ginas.utils.validation.ValidatorFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DefaultHoldingAreaService<T> implements HoldingAreaService {

    public static String IMPORT_FAILURE = "ERROR";

    public static String HOLDING_AREA_LOCATION = "Staging Area";

    @Autowired
    ImportMetadataRepository metadataRepository;

    @Autowired
    ImportDataRepository importDataRepository;

    @Autowired
    RawImportDataRepository rawImportDataRepository;

    @Autowired
    KeyValueMappingRepository keyValueMappingRepository;

    //@Autowired
    private TextIndexerFactory tif;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    private ImportValidationRepository importValidationRepository;

    @Autowired
    private GsrsValidatorFactory validatorFactoryService;

    @Autowired
    private ImportMetadataLegacySearchService importMetadataLegacySearchService;

    @Autowired
    private IndexValueMakerFactory factory;

    @Value("${ix.home:ginas.ix}")
    private String textIndexerFactorDefaultDir;

    private ValidatorFactory validatorFactory;

    private TextIndexer indexer;

    private final String context;

    private MatchableCalculationConfig matchableCalculationConfig;

    public final static String CURRENT_SOURCE = "Holding Area";

    private Map<String, HoldingAreaEntityService> _entityServiceRegistry = new HashMap<>();

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public DefaultHoldingAreaService(String context) {
        this.context = Objects.requireNonNull(context, "context can not be null");
    }

    /*
    For a unit test... temporarily
     */
    public DefaultHoldingAreaService() {
        this.context = "substances";
    }

    @PostConstruct
    public void setupIndexer() {
        log.trace("starting setupIndexer");
        if (tif != null) {
            //indexer = tif.getDefaultInstance();
            indexer =tif.getInstance(new File("imports"));
            log.trace("got indexer from tif.getDefaultInstance()");
        } else {
            try {
                log.trace("going to create indexerFactory");
                TextIndexerFactory indexerFactory = new TextIndexerFactory();

                AutowireHelper.getInstance().autowireAndProxy(indexerFactory);
                //indexer = indexerFactory.getDefaultInstance();
                log.trace("textIndexerFactorDefaultDir: {}", textIndexerFactorDefaultDir);
                indexer =indexerFactory.getInstance(new File(textIndexerFactorDefaultDir +"/imports"));
                log.trace("got indexer from indexerFactory.getDefaultInstance(): " + indexer);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }


    @Override
    public String createRecord(ImportRecordParameters parameters) {
        if( indexer==null) setupIndexer();
        Objects.requireNonNull(indexer, "need a text indexer!");
        ImportData data = new ImportData();
        data.setData(parameters.getJsonData());
        UUID recordId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        data.setRecordId(recordId);
        data.setVersion(1);
        data.setInstanceId(instanceId);
        data.setSaveDate(new Date());
        data.setEntityClassName(parameters.getEntityClassName());
        Objects.requireNonNull(importDataRepository, "importDataRepository is required");
        ImportData saved = importDataRepository.saveAndFlush(data);

        ImportMetadata metadata = new ImportMetadata();
        metadata.setRecordId(recordId);
        metadata.setInstanceId(instanceId);
        metadata.setEntityClassName(parameters.getEntityClassName());
        metadata.setImportStatus(ImportMetadata.RecordImportStatus.staged);
        metadata.setProcessStatus(ImportMetadata.RecordProcessStatus.loaded);
        metadata.setValidationStatus(ImportMetadata.RecordValidationStatus.pending);
        metadata.setVersion(1);
        metadata.setImportType(ImportMetadata.RecordImportType.create);
        metadata.setSourceName(parameters.getSource());
        metadata.setVersionStatus(ImportMetadata.RecordVersionStatus.current);
        metadata.setVersionCreationDate(new Date());
        metadata.setDataFormat(parameters.getFormatType());
        metadataRepository.saveAndFlush(metadata);
        EntityUtils.EntityWrapper<ImportMetadata> wrapper = EntityUtils.EntityWrapper.of(metadata);
        try {
            indexer.add(wrapper);
            log.trace("indexer.add called");
        } catch (IOException e) {
            log.error("Error indexing import metadata to index", e);
        }
        //log.trace("indexer.add skippedindexer.add skipped");
        //update processing status after every step

        if (parameters.getRawDataSource() != null) {
            try {
                saveRawData(parameters.getRawDataSource(), recordId);
            } catch (IOException exception) {
                log.error("Error processing raw data", exception);
            }
        }

        Object domainObject;
        try {
            log.trace("going deserialize object of class {}", parameters.getEntityClassName());
            log.trace(parameters.getJsonData());
            domainObject = deserializeObject(parameters.getEntityClassName(), parameters.getJsonData());
        } catch (JsonProcessingException e) {
            log.error("Error deserializing imported object.", e);
            return IMPORT_FAILURE;
        }
        if (domainObject == null) {
            log.warn("null domainObject!");
            return recordId.toString();
        }
        log.trace("parameters.getEntityClassName(): {}; domainObject.getClass().getName(): {}", parameters.getEntityClassName(), domainObject.getClass().getName());
        _entityServiceRegistry.get(parameters.getEntityClassName()).IndexEntity(indexer, domainObject);

        ValidationResponse response = _entityServiceRegistry.get(parameters.getEntityClassName()).validate(domainObject);
        domainObject = response.getNewObject();
        ImportMetadata.RecordValidationStatus overallStatus = ImportMetadata.RecordValidationStatus.unparseable;
        if (response != null) {
            List<UUID> savedResult = persistValidationInfo(response, 1, instanceId);
            overallStatus = getOverallValidationStatus(response);
        }
        //save the updated object
        try {
            log.trace("going to save updated domain object post-validation");
            data.setData(serializeObject(domainObject));
            data.setVersion(data.getVersion() + 1);
            data.setInstanceId(UUID.randomUUID());
            data.setSaveDate(new Date());
            importDataRepository.save(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        log.trace("overallStatus: " + overallStatus);
        updateImportValidationStatus(instanceId, overallStatus);

        List<MatchableKeyValueTuple> definitionalValueTuples = getMatchables(domainObject);
        definitionalValueTuples.forEach(t -> log.trace("key: {}, value: {}", t.getKey(), t.getValue()));
        persistDefinitionalValues(definitionalValueTuples, instanceId, recordId);
        updateRecordImportStatus(instanceId, ImportMetadata.RecordImportStatus.staged);

        handleIndexing(metadata, saved);
        //event driven: each step in process sends an event (pub/sub) look in ... indexing
        //  validation, when done would trigger the next event
        //  event manager type of thing
        // passively: daemon running in background looks for records with a given status and then performs
        // the next step
        // will
        //todo: run duplicate check
        try {
            MatchedRecordSummary summary = findMatches(domainObject.getClass().getName(), definitionalValueTuples);
            log.trace("Matches: ");
            summary.getMatches().forEach(m -> {
                log.trace("Matching key: {} = {}", m.getTupleUsedInMatching().getKey(), m.getTupleUsedInMatching().getValue());
                if (m.getMatchingRecords().size() == 0) {
                    log.trace(" 0 matching records");

                } else {
                    m.getMatchingRecords().forEach(r -> log.trace("   location: {} record Id: {}; key that matched: {}", r.getSourceName(), r.getRecordId(),
                            r.getMatchedKey()));
                }

            });
        } catch (ClassNotFoundException e) {
            log.error("Error looking for matches", e);
        }

        return saved.getRecordId().toString();
    }

    private void handleIndexing(ImportMetadata importMetadata, ImportData importData){
        log.trace("Here is where we index facets for the ImportMetadata object");
        EntityUtils.EntityWrapper entityWrapper = EntityUtils.EntityWrapper.of(importMetadata);
        UUID reindexUuid = UUID.randomUUID();
        ReindexEntityEvent event = new ReindexEntityEvent(reindexUuid, entityWrapper.getKey(), Optional.of(entityWrapper), true);
        applicationEventPublisher.publishEvent(event);
        log.trace("published event for metadata");
        if( importData!=null) {
            EntityUtils.EntityWrapper entityWrapperData = EntityUtils.EntityWrapper.of(importData);
            UUID reindexUuidData = UUID.randomUUID();
            ReindexEntityEvent eventData = new ReindexEntityEvent(reindexUuidData, entityWrapperData.getKey(), Optional.of(entityWrapperData), true);
            applicationEventPublisher.publishEvent(eventData);
            log.trace("published event for data");
        }
    }

    @Override
    public String updateRecord(String recordId, String jsonData) {
        //locate the latest record
        List<ImportData> importData= importDataRepository.retrieveDataForRecord(UUID.fromString(recordId));
        if(importData==null || importData.isEmpty()){
            return "No data found";
        }
        ImportData latestExisting = importData.stream().max(Comparator.comparing(ImportData::getVersion)).get();
        log.trace("located object with latest version {}", latestExisting.getVersion());
        ImportData newVersion = latestExisting.toBuilder()
                .instanceId(UUID.randomUUID())
                .version(latestExisting.getVersion()+1)
                .data(jsonData)
                .build();
        log.trace("cloned");
        TransactionTemplate transactionDelete = new TransactionTemplate(transactionManager);
        transactionDelete.executeWithoutResult(r -> importDataRepository.save(newVersion));

        return String.format("updated data object");
    }

    @Override
    public ImportMetadata getImportMetaData(String recordId, int version) {
        if(version==0) {
            List<ImportMetadata> matchingRecords = metadataRepository.retrieveByID(UUID.fromString(recordId));
            if( matchingRecords !=null && matchingRecords.size()>0) {
                return matchingRecords.stream().min(Comparator.comparing(ImportMetadata::getVersion)).get();
            }
            return null;
        }
        return metadataRepository.retrieveByIDAndVersion(UUID.fromString(recordId), version);
    }

    @Override
    public ImportMetadata getImportMetaData(String instanceId) {
        return metadataRepository.retrieveByInstanceID(UUID.fromString(instanceId));
    }

    @Override
    public ImportData getImportDataByInstanceIdOrRecordId(String id, int version) {
        log.trace("getImportDataByInstanceIdOrRecordId starting. ID: {}", id);
        //first, look for ImportData directly
        List<ImportData> importDataList = getImportData(id);
        if( importDataList==null || importDataList.isEmpty()){
            log.trace("no ImportData found; looking for ImportMetaData");
            ImportMetadata metadata= getImportMetaData(id);
            if( metadata!=null && metadata.getRecordId()!=null){
                importDataList= getImportData(metadata.getRecordId().toString());
            }
        }
        if( importDataList!=null && !importDataList.isEmpty()){
            Optional<ImportData> data;
            if( version<=0) {
                log.trace("no version supplied; looking for latest item");
                data = importDataList.stream().min(Comparator.comparing(ImportData::getVersion));
            } else {
                log.trace("looking for specific version {}",  version);
                data = importDataList.stream().filter(d->d.getVersion()==version).findFirst();
            }

            if(data.isPresent()) {
                return data.get();
            }
        }
        return null;
    }

    @Override
    public void deleteRecord(String recordId, int version) {
        log.trace("starting deleteRecord. recordId: {}; version: {}", recordId, version);
        UUID id = UUID.fromString(recordId);

        //need to retrieve instance IDs so individual validations can be deleted
        List<UUID> instanceIds = importDataRepository.findInstancesForRecord(id);

        TransactionTemplate transactionDelete = new TransactionTemplate(transactionManager);
        if( version>0) {
            log.trace("deleting specific version {}", version);
            transactionDelete.executeWithoutResult(r -> {
                importDataRepository.deleteByRecordIdAndVersion(id, version);
                log.trace("completed importDataRepository.deleteByIdAndVersion");
                metadataRepository.deleteByRecordIdAndVersion(id, version);
                log.trace("completed metadataRepository.deleteByIdAndVersion");
                //Have we deleted ALL records with that ID?  If so, clean up
                if( !importDataRepository.existsById(id)) {
                    deleteRawDataAndMappings(id);
                }
            });
        } else {
            transactionDelete.executeWithoutResult(r -> {
                importDataRepository.deleteByRecordId(id);
                log.trace("completed importDataRepository.deleteById");
                metadataRepository.deleteByRecordId(id);
                log.trace("completed metadataRepository.deleteById");
                deleteRawDataAndMappings(id);

                instanceIds.forEach(i -> {
                    log.trace("going to call importValidationRepository.deleteByInstanceId with id {}", i);
                    importValidationRepository.deleteByInstanceId(i);
                });
            });
        }
    }

    private void deleteRawDataAndMappings(UUID id){
        rawImportDataRepository.deleteByRecordId(id);
        log.trace("completed rawImportDataRepository.deleteByRecordId");
        if( keyValueMappingRepository.findRecordsByRecordId(id) != null
                && !keyValueMappingRepository.findRecordsByRecordId(id).isEmpty()) {
            keyValueMappingRepository.deleteByRecordId(id);
            log.trace("completed keyValueMappingRepository.deleteByRecordId");
        }
        else {
            log.trace("no keyValueMappingRepository records to delete");
        }

    }

    @Override
    public <T> void registerEntityService(HoldingAreaEntityService<T> service) {
        log.trace("registered entity service class {} for entity class {}", service.getClass().getName(),
                service.getEntityClass().getName());
        _entityServiceRegistry.put(service.getEntityClass().getName(), service);
    }

    @Override
    public <T> List<MatchableKeyValueTuple> calculateMatchables(T object) {
        log.trace("in calculateMatchables");
        return _entityServiceRegistry.get(object.getClass().getName()).extractKVM(object);
    }

    @Override
    public <T> ValidationResponse<T> validateRecord(String entityClass, String json) {

        try {
            Object object = deserializeObject(entityClass, json);
            return _entityServiceRegistry.get(entityClass).validate(object);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <T> SearchResult findRecords(SearchRequest searchRequest, Class<T> cls) {
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        return transactionSearch.execute(ts -> {
            try {
                importMetadataLegacySearchService = new ImportMetadataLegacySearchService(metadataRepository);
                SearchResult searchResult = importMetadataLegacySearchService.search(searchRequest.getQuery(), searchRequest.getOptions());
                return searchResult;
            } catch (Exception e) {
                log.error("Error running search", e);
            }
            return null;
        });
    }

    @Override
    public MatchedRecordSummary findMatches(String entityClass, List<MatchableKeyValueTuple> recordMatchables) throws ClassNotFoundException {
        Class objectClass = Class.forName(entityClass);
        MatchedRecordSummary summary = new MatchedRecordSummary();
        summary.setQuery(recordMatchables);
        //todo: test this
        recordMatchables.forEach(m -> {
            List<KeyValueMapping> mappings = keyValueMappingRepository.findMappingsByKeyAndValue(m.getKey(), m.getValue());
            log.trace("matches for {}={}[layer: {}] (total: {}):", m.getKey(), m.getValue(), m.getLayer(), mappings.size());
            if (m.getValue() == null || m.getValue().length() == 0) {
                log.trace("skipping matchable without value");
                return;
            }
            List<MatchedKeyValue.MatchingRecordReference> matches = mappings.stream()
                    .map(ma -> MatchedKeyValue.MatchingRecordReference.builder()
                            .recordId(EntityUtils.Key.of(objectClass, ma.getInstanceId()))
                            .sourceName(ma.getDataLocation())
                            .matchedKey(m.getKey())
                            .build())
                    .collect(Collectors.toList());
            MatchedKeyValue match = MatchedKeyValue.builder()
                    .tupleUsedInMatching(m)
                    .matchingRecords(matches)
                    .build();
            summary.getMatches().add(match);
        });
        return summary;
    }

    @Override
    public List<UUID> getInstancesForRecord(String recordId) {
        return importDataRepository.findInstancesForRecord(UUID.fromString(recordId));
    }

    @Override
    public List<ImportData> getImportData(String recordId) {
        return importDataRepository.retrieveDataForRecord(UUID.fromString(recordId));
    }

    @Override
    public String getInstanceData(String instanceId) {
        return importDataRepository.retrieveByInstanceID(UUID.fromString(instanceId));
    }

    @Override
    public <T> String persistEntity(String instanceId){
        log.trace("in persistEntity, instanceId: {}", instanceId);
        Optional<ImportData> data= importDataRepository.findById(UUID.fromString(instanceId));
        if( data.isPresent()) {
            log.trace("found data");
            String entityJson = data.get().getData();
            String entityType = data.get().getEntityClassName();
            try {
                Object domainObject = deserializeObject(entityType, entityJson);
                ValidationResponse<T> response = validateRecord(entityType, entityJson);
                List<ValidationMessage> messages = response.getValidationMessages();
                if (messages.stream().noneMatch(m -> m.isError())) {
                    Object savedObject = _entityServiceRegistry.get(entityType).persistEntity(domainObject);
                    if (savedObject instanceof GinasCommonData) {
                        metadataRepository.updateRecordImportStatus(UUID.fromString(instanceId), ImportMetadata.RecordImportStatus.imported);
                        return ((GinasCommonData) savedObject).uuid.toString();
                    }
                    return "Object saved!";
                }
                return "One or more errors exist. Please validate and take action!";
            } catch (JsonProcessingException e) {
                log.error("Error in persistEntity", e);
            }
        }
        return "";
    }

    @Override
    public <T> T retrieveEntity(String entityType, String entityId) {
        log.trace("going to retrieve entity of type {} - id {}", entityType, entityId);
        return (T) _entityServiceRegistry.get(entityType).retrieveEntity(entityId);
    }

    @Override
    public <T> GsrsEntityService.ProcessResult<T> persistPermanentEntity(String entityType, T entity) {
        return _entityServiceRegistry.get(entityType).persistEntity(entity);
    }

    @Override
    public <T> ValidationResponse<T> validateInstance(String instanceId) {
        log.trace("in validateInstance, instanceId: {}", instanceId);
        Optional<ImportData> data= importDataRepository.findById(UUID.fromString(instanceId));
        if( data.isPresent()) {
            log.trace("found data");
            String entityJson = data.get().getData();
            String entityType = data.get().getEntityClassName();
            return validateRecord(entityType, entityJson);
        }
        return null;
    }

    @SneakyThrows
    @Override
    public MatchedRecordSummary findMatchesForJson(String qualifiedEntityType, String entityJson) {
        log.trace("starting findMatchesForJson with {}", qualifiedEntityType);
        Object domainObject;
        try {
            log.trace("going deserialize object of class {}", qualifiedEntityType);
            log.trace(entityJson);
            domainObject = deserializeObject(qualifiedEntityType, entityJson);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing imported object.", e);
            return new MatchedRecordSummary();
        }
        if (domainObject == null) {
            log.warn("null domainObject!");
            return new MatchedRecordSummary();
        }

        //TODO: extend this to other types of objects
        List<MatchableKeyValueTuple> definitionalValueTuples = getMatchables(domainObject);
        MatchedRecordSummary matches = findMatches(qualifiedEntityType, definitionalValueTuples);
        return matches;
    }

    public String getContext() {
        return context;
    }

    public void setMatchableCalculationConfig(MatchableCalculationConfig matchableCalculationConfig) {
        this.matchableCalculationConfig = matchableCalculationConfig;
    }

    private <T> List<MatchableKeyValueTuple> getMatchables(T entity) {
        String lookupClass = entity.getClass().getName();
        log.trace("getMatchables looking for extractor for class {}", lookupClass);
        if (!_entityServiceRegistry.containsKey(lookupClass)) {
            lookupClass = entity.getClass().getSuperclass().getName();
            log.trace(" getMatchables using base class {}", lookupClass);
        }
        return _entityServiceRegistry.get(lookupClass).extractKVM(entity);
    }


    private String saveRawData(InputStream rawData, UUID id) throws IOException {
        RawImportData rawImportData = new RawImportData();

        rawImportData.setRawData(IOUtils.toByteArray(rawData));
        rawImportData.setRecordId(id);
        RawImportData savedRawImportData = rawImportDataRepository.saveAndFlush(rawImportData);
        return savedRawImportData.getRecordId().toString();
    }

    /*
    question: does this method need to return something?
     */
    private void persistDefinitionalValues(List<MatchableKeyValueTuple> definitionalValues, UUID instanceId, UUID recordId) {

        definitionalValues.forEach(kv -> {
            KeyValueMapping mapping = new KeyValueMapping();
            mapping.setKey(kv.getKey());
            mapping.setValue(kv.getValue());
            mapping.setInstanceId(instanceId);
            mapping.setRecordId(recordId);
            mapping.setEntityClass(context);
            mapping.setDataLocation(HOLDING_AREA_LOCATION);
            keyValueMappingRepository.saveAndFlush(mapping);
            //index for searching
            EntityUtils.EntityWrapper<KeyValueMapping> wrapper = EntityUtils.EntityWrapper.of(mapping);
            /*try {
                indexer.add(wrapper);
            } catch (IOException e) {
                log.error("Error indexing import metadata to index", e);
            }*/
        });
    }


    private <T> List<UUID> persistValidationInfo(ValidationResponse<T> validationResponse, int version, UUID instanceId) {
        List<UUID> validationIds = new ArrayList<>();
        validationResponse.getValidationMessages().forEach(m -> {
            ImportValidation.ImportValidationType type = ImportValidation.ImportValidationType.info;
            if (m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR) {
                type = ImportValidation.ImportValidationType.error;
            } else if (m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING) {
                type = ImportValidation.ImportValidationType.warning;
            }
            UUID validationId = UUID.randomUUID();
            ImportValidation validation = ImportValidation.builder()
                    .ValidationId(validationId)
                    .ValidationDate(new Date())
                    .ValidationType(type)
                    .ValidationJson(validationResponse.toString())
                    .ValidationMessage(m.getMessage())
                    .version(version)
                    .instanceId(instanceId)
                    .build();

            importValidationRepository.saveAndFlush(validation);
            validationIds.add(validationId);
        });
        return validationIds;
    }


    private void updateImportValidationStatus(UUID instanceId, ImportMetadata.RecordValidationStatus status) {
        TransactionTemplate transactionUpdate = new TransactionTemplate(transactionManager);
        transactionUpdate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionUpdate.executeWithoutResult(c -> metadataRepository.updateRecordValidationStatus(instanceId, status));
    }

    @Override
    public void updateRecordImportStatus(UUID instanceId, ImportMetadata.RecordImportStatus status) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        //todo: should this be version-specific?
        transactionTemplate.executeWithoutResult(c -> metadataRepository.updateRecordImportStatus(instanceId, status));
    }

    private ImportMetadata.RecordValidationStatus getOverallValidationStatus(ValidationResponse validationResponse) {
        ImportMetadata.RecordValidationStatus response = ImportMetadata.RecordValidationStatus.valid;

        if (validationResponse.getValidationMessages().stream().anyMatch(m -> ((ValidationMessage) m).getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR)) {
            response = ImportMetadata.RecordValidationStatus.error;
        } else if (validationResponse.getValidationMessages().stream().anyMatch(m -> ((ValidationMessage) m).getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)) {
            response = ImportMetadata.RecordValidationStatus.warning;
        }

        return response;
    }

    @Override
    public void setIndexer(TextIndexer indexer) {
        log.trace("setIndexer");
        this.indexer = indexer;
    }

    @Override
    public Object deserializeObject(String entityClassName, String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        if (_entityServiceRegistry.containsKey(entityClassName)) {
            return _entityServiceRegistry.get(entityClassName).parse(node);
        } else {
            log.error("No entity service for class {}", entityClassName);
            return null;
        }
    }

    private String serializeObject(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

}
