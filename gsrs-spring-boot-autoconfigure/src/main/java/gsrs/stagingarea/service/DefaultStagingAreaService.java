package gsrs.stagingarea.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.GsrsFactoryConfiguration;
import gsrs.events.ReindexEntityEvent;
import gsrs.stagingarea.model.*;
import gsrs.stagingarea.repository.*;
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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class DefaultStagingAreaService<T> implements StagingAreaService {

    public static String IMPORT_FAILURE = "ERROR";

    public static String STAGING_AREA_LOCATION = "Staging Area";

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

    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;

    @Value("${ix.home:ginas.ix}")
    private String textIndexerFactorDefaultDir;

    //private ValidatorFactory validatorFactory;

    private TextIndexer indexer;

    private Map<String, StagingAreaEntityService> _entityServiceRegistry = new HashMap<>();

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

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
        //step 1 - persist raw object JSON to staging area
        ImportData data = new ImportData();
        data.setData(parameters.getJsonData());
        UUID recordId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        data.setRecordId(recordId);
        data.setVersion(1);
        data.setInstanceId(instanceId);
        data.setSaveDate(TimeUtil.getCurrentDate());
        data.setEntityClassName(parameters.getEntityClassName());
        Objects.requireNonNull(importDataRepository, "importDataRepository is required");
        ImportData saved = importDataRepository.saveAndFlush(data);

        //step 2 - save metadata
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
        metadata.setImportAdapter(parameters.getAdapterName());
        if(parameters.getImportingUser()!=null){
            metadata.setImportedBy( parameters.getImportingUser());
        }else{
            log.warn("Unable to retrieve current user!");
        }

        metadataRepository.saveAndFlush(metadata);

        //step 3: save raw data, when available
        if (parameters.getRawDataSource() != null) {
            try {
                saveRawData(parameters.getRawDataSource(), recordId);
            } catch (IOException exception) {
                log.error("Error processing raw data", exception);
            }
        }

        //deserialize
        Object domainObject;
        try {
            log.trace("going deserialize object of class {}", parameters.getEntityClassName());
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

        //step 4: validate
        ImportMetadata.RecordValidationStatus overallStatus = ImportMetadata.RecordValidationStatus.unparseable;

        boolean performValidation=true;
        boolean performIndexing=true;
        boolean performMatching=true;
        log.trace("parameters.getSettings(): {}", parameters.getSettings());
        if(parameters.getSettings()!= null && parameters.getSettings().hasNonNull("rarelyUsedSettings")) {

            ObjectNode rareSettings = (ObjectNode) parameters.getSettings().get("rarelyUsedSettings");
            log.trace("unpacking rarelyUsedSettings. ");
            if(rareSettings.hasNonNull("skipValidation") && rareSettings.get("skipValidation").asBoolean()) {
                performValidation=false;
            }
            if(rareSettings.hasNonNull("skipIndexing") && rareSettings.get("skipIndexing").asBoolean()) {
                performIndexing=false;
            }
            if(rareSettings.hasNonNull("skipMatching") && rareSettings.get("skipMatching").asBoolean()) {
                performMatching=false;
            }
        }
        if( performValidation) {
            log.trace("going to validate. registry has item? {}", _entityServiceRegistry.containsKey(parameters.getEntityClassName()));
            ValidationResponse response = _entityServiceRegistry.get(parameters.getEntityClassName()).validate(domainObject);
            if (response != null) {
                domainObject = response.getNewObject();
                persistValidationInfo(response, 1, instanceId);
                overallStatus = getOverallValidationStatus(response);
                try {
                    importDataRepository.updateDataByRecordIdAndVersion(recordId, 1, serializeObject(domainObject));
                    log.trace("updating record after validation");
                } catch (JsonProcessingException e) {
                    log.error("Error serializing validated substance", e);
                }
            }

            log.trace("overallStatus: " + overallStatus);
            updateImportValidationStatus(recordId, overallStatus);
        }
        //step 5: matchables

        updateRecordImportStatus(recordId, ImportMetadata.RecordImportStatus.staged);

        if(performMatching){
            log.trace("going to match");
            List<MatchableKeyValueTuple> definitionalValueTuples = getMatchables(domainObject);
            definitionalValueTuples.forEach(t -> log.trace("key: {}, value: {}", t.getKey(), t.getValue()));
            persistDefinitionalValues(definitionalValueTuples, instanceId, recordId, parameters.getEntityClassName());

            //event driven: each step in process sends an event (pub/sub) look in ... indexing
            //  validation, when done would trigger the next event via
            //  event manager type of thing
            // passively: daemon running in background looks for records with a given status and then performs
            // the next step
            // will
            //try {
            //    MatchedRecordSummary summary = findMatches(domainObject.getClass().getName(), definitionalValueTuples, recordId.toString());
                //log.trace("Matches: ");
                //summary.getMatches().forEach(m -> {
                    //log.trace("Matching key: {} = {}", m.getTupleUsedInMatching().getKey(), m.getTupleUsedInMatching().getValue());
                //});
            //} catch (ClassNotFoundException e) {
            //    log.error("Error looking for matches", e);
            //}
        }
        if( performIndexing ) {
            log.trace("going to index");
            handleIndexing(metadata);
        }

        return saved.getRecordId().toString();
    }

    private void handleIndexing(ImportMetadata importMetadata){
        log.trace("Here is where we index facets for the ImportMetadata object");
        EntityUtils.EntityWrapper entityWrapper = EntityUtils.EntityWrapper.of(importMetadata);
        UUID reindexUuid = UUID.randomUUID();
        ReindexEntityEvent event = new ReindexEntityEvent(reindexUuid, entityWrapper.getKey(), Optional.of(entityWrapper));
        applicationEventPublisher.publishEvent(event);
        log.trace("published event for metadata");
    }

    @Override
    public String updateRecord(String recordId, String jsonData) {
        log.trace("starting in updateRecord");
        //locate the latest record
        List<ImportData> importData= importDataRepository.retrieveDataForRecord(UUID.fromString(recordId));
        if(importData==null || importData.isEmpty()){
            log.trace("no data found for id {}", recordId);
            return "No data found";
        }
        ImportData latestExisting = importData.stream().max(Comparator.comparing(ImportData::getVersion)).get();
        log.trace("located object with latest version {}", latestExisting.getVersion());
        UUID latestInstanceId=UUID.randomUUID();

        ImportMetadata importMetadata = metadataRepository.retrieveByRecordID(UUID.fromString(recordId));
        String cleanJson;
        try {
            //deserializing and re-serializing will allow us to reset the '_name' field of substances
            Object data = deserializeObject(importMetadata.getEntityClassName(), jsonData);
            cleanJson= serializeObject(data);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing JSON", e);
            throw new RuntimeException(e);
        }
        ImportData newVersion = latestExisting.toBuilder()
                .instanceId(latestInstanceId)
                .version(latestExisting.getVersion()+1)
                .data(cleanJson)
                .saveDate( new Date())
                .build();
        log.trace("cloned");

        log.trace("retrieved importMetadata with {} mappings and {} validations", importMetadata.keyValueMappings.size(), importMetadata.validations.size());
        TransactionTemplate transactionDelete = new TransactionTemplate(transactionManager);
        transactionDelete.executeWithoutResult(r -> {
            importDataRepository.save(newVersion);
            try {
                propagateUpdate(importMetadata, jsonData, importMetadata.getEntityClassName(), latestInstanceId);
                //retrieve importMetadata again because it has changed
                ImportMetadata updatedImportMetadata= metadataRepository.retrieveByRecordID(UUID.fromString(recordId));
                //change the instanceID _after_ handleUpdate that uses the old value to clean out related  data
                updatedImportMetadata.setInstanceId(latestInstanceId);
                metadataRepository.setInstanceIdForRecord(latestInstanceId, UUID.fromString(recordId));
                //ImportMetadata saved=metadataRepository.save(updatedImportMetadata);
                //metadataRepository.saveAndFlush(updatedImportMetadata);
                //handleIndexing(updatedImportMetadata);
                //log.trace("saved ImportMetadata with recordID {} and instanceId {}; latestInstanceId: {}; saved: {}", updatedImportMetadata.getRecordId(),
                 //       updatedImportMetadata.getInstanceId(), latestInstanceId, saved.getInstanceId());
                ImportMetadata reretrievedIM = metadataRepository.retrieveByRecordID(updatedImportMetadata.getRecordId());
                log.trace("from reretrievedIM: {}", reretrievedIM.getInstanceId());
            } catch (JsonProcessingException e) {
                log.error("Error updating import metadata!", e);
                throw new RuntimeException(e);
            }
        } );
        return String.format("updated data object");
    }

    @Override
    public ImportMetadata getImportMetaData(String recordId, int version) {
        if(version==0) {
            return metadataRepository.retrieveByRecordID(UUID.fromString(recordId));
        }
        return metadataRepository.retrieveByIDAndVersion(UUID.fromString(recordId), version);
    }

    @Override
    public ImportMetadata getImportMetaData(String instanceId) {
        return metadataRepository.retrieveByRecordID(UUID.fromString(instanceId));
    }

    @Override
    public ImportData getImportDataByInstanceIdOrRecordId(String id, int version) {
        log.trace("getImportDataByInstanceIdOrRecordId starting. ID: {}", id);
        //first, look for ImportData directly -- assuming recordID
        List<ImportData> importDataList = getImportData(id);

        if( importDataList==null || importDataList.isEmpty()){

            log.trace("no ImportData found by recordId; looking by instanceId");
            try {
                Optional<ImportData> dataItem = importDataRepository.findById(UUID.fromString(id));
                if (dataItem.isPresent()) {
                    importDataList = new ArrayList<>();
                    importDataList.add(dataItem.get());
                }
            } catch (NoSuchMethodError error) {
                //have observed this error several times when attempting to retrieve a non-existent ID
                log.warn("error retrieving ImportData for id {}", id);
            }
        }

        if( importDataList!=null && !importDataList.isEmpty()){
            Optional<ImportData> data;
            if( version<=0) {
                log.trace("no version supplied; looking for latest item");
                data = importDataList.stream().max(Comparator.comparing(ImportData::getVersion));
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
    public <T> void registerEntityService(StagingAreaEntityService<T> service) {
        log.trace("registered entity service class {} for entity class {}", service.getClass().getName(),
                service.getEntityClass().getName());
        _entityServiceRegistry.put(service.getEntityClass().getName(), service);
    }

    @Override
    public <T> StagingAreaEntityService<T> getEntityService(String entityName) {
        return _entityServiceRegistry.get(entityName);
    }

    @Override
    public <T> List<MatchableKeyValueTuple> calculateMatchables(T object) {
        log.trace("in calculateMatchables of class {}", object.getClass().getName());
        StagingAreaEntityService entityService = getStagingAreaEntityService(object.getClass());
        if( entityService!=null) {
            log.trace("found entity service");
            return entityService.extractKVM(object);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public <T> ValidationResponse<T> validateRecord(String entityClass, String json) {

        try {
            Class requiredClass =Class.forName(entityClass);
            Object object = deserializeObject(entityClass, json);
            if( !requiredClass.isAssignableFrom(object.getClass())) {
                log.error("Validating object of type {} is not supported", object.getClass().getName());
                return new ValidationResponse<T>();
            }
            log.trace("Going to validate object of type {} because it belongs to {}", object.getClass().getName(),
                    entityClass);
            return _entityServiceRegistry.get(entityClass).validate(object);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <T> SearchResult findRecords(SearchRequest searchRequest, Class<T> cls) {
        log.trace("in findRecords");
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        return transactionSearch.execute(ts -> {
            try {
                log.trace("going to instantiate importMetadataLegacySearchService");
                importMetadataLegacySearchService = new ImportMetadataLegacySearchService(metadataRepository);
                AutowireHelper.getInstance().autowire(importMetadataLegacySearchService);
                SearchResult searchResult = importMetadataLegacySearchService.search(searchRequest.getQuery(), searchRequest.getOptions());
                return searchResult;
            } catch (Exception e) {
                log.error("Error running search", e);
            }
            return null;
        });
    }

    @Override
    public MatchedRecordSummary findMatches(String entityClass, List<MatchableKeyValueTuple> recordMatchables,
                                            String startingRecordId) throws ClassNotFoundException {
        log.trace("in findMatches, entityClass: {}", entityClass);
        Class objectClass = Class.forName(entityClass);
        MatchedRecordSummary summary = new MatchedRecordSummary();
        summary.setQuery(recordMatchables);
        //todo: test this
        recordMatchables.forEach(m -> {
            List<KeyValueMapping> mappings = keyValueMappingRepository.findMappingsByKeyAndValue(m.getKey(), m.getValue());
            if (m.getValue() != null && m.getValue().length() > 0) {
                log.trace("matches for {}={}[layer: {}] (total: {}):", m.getKey(), m.getValue(), m.getLayer(), mappings.size());
                List<MatchedKeyValue.MatchingRecordReference> matches = mappings.stream()
                        .filter(ma->startingRecordId==null || !ma.getRecordId().toString().equals(startingRecordId))
                        .map(ma -> {
                            MatchedKeyValue.MatchingRecordReference.MatchingRecordReferenceBuilder builder = MatchedKeyValue.MatchingRecordReference.builder();
                            builder
                                    .sourceName(ma.getDataLocation())
                                    .matchedKey(m.getKey());
                            if (ma.getRecordId() != null) {
                                builder.recordId(EntityUtils.Key.of(objectClass, ma.getRecordId()));
                            } else {
                                log.trace("skipping item without an recordID");
                            }
                            return builder.build();
                        })
                        .collect(Collectors.toList());

                MatchedKeyValue match = MatchedKeyValue.builder()
                        .tupleUsedInMatching(m)
                        .matchingRecords(matches)
                        .build();
                summary.getMatches().add(match);
            }
        });
        return summary;
    }

    @Override
    public MatchedRecordSummary findMatches(ImportMetadata importMetadata) throws ClassNotFoundException, JsonProcessingException {
        log.trace("starting findMatches of ImportMetadata. Instance ID: {}", importMetadata.getInstanceId());
        //first, retrieve the latest Object JSON
        List<ImportData> importDataList = importDataRepository.retrieveDataForRecord(importMetadata.getRecordId());
        ImportData latestExisting = importDataList.stream().max(Comparator.comparing(ImportData::getVersion)).get();

        String jsonData= latestExisting.getData();
        log.trace("Got JSON for metadata with {}", importMetadata.getRecordId());
        //deserialize
        T domainObject = (T) deserializeObject(importMetadata.getEntityClassName(), jsonData);
        log.trace("deserialized");
        List<MatchableKeyValueTuple> matchableKeyValueTuples =calculateMatchables(domainObject);
        log.trace("calculated latest matchables");
        matchableKeyValueTuples.forEach(m-> log.trace("key: {} = value: {}", m.getKey(), m.getValue()));
        return findMatches(domainObject.getClass().getName(), matchableKeyValueTuples, importMetadata.getRecordId().toString());
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
    public <T> T retrieveEntity(String entityType, String entityId) {
        log.trace("going to retrieve entity of type {} - id {}", entityType, entityId);
        return (T) _entityServiceRegistry.get(entityType).retrieveEntity(entityId);
    }

    @Override
    public <T> GsrsEntityService.ProcessResult<T> saveEntity(String entityType, T entity, boolean brandNew) {
        return _entityServiceRegistry.get(entityType).persistEntity(entity, brandNew);
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
    public MatchedRecordSummary findMatchesForJson(String qualifiedEntityType, String entityJson, String startingRecordId) {
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
        MatchedRecordSummary matches = findMatches(qualifiedEntityType, definitionalValueTuples, startingRecordId);
        return matches;
    }

    /*public void setMatchableCalculationConfig(MatchableCalculationConfig matchableCalculationConfig) {
        this.matchableCalculationConfig = matchableCalculationConfig;
    }*/

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
    private void persistDefinitionalValues(List<MatchableKeyValueTuple> definitionalValues, UUID instanceId, UUID recordId,
                                           String matchedEntityClass) {

        definitionalValues.forEach(kv -> {

            KeyValueMapping mapping = new KeyValueMapping();
            mapping.setKey(kv.getKey());
            String valueToStore = kv.getValue();
            if( valueToStore !=null && valueToStore.length()> KeyValueMapping.MAX_VALUE_LENGTH) {
                valueToStore = valueToStore.substring(0, KeyValueMapping.MAX_VALUE_LENGTH-1);
            }
            mapping.setValue(valueToStore);
            mapping.setInstanceId(instanceId);
            mapping.setRecordId(recordId);
            mapping.setEntityClass(matchedEntityClass);
            mapping.setDataLocation(STAGING_AREA_LOCATION);
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

    @Override
    public List<ImportValidation> retrieveValidationForInstance(UUID instanceId) {
        return importValidationRepository.retrieveValidationsByInstanceId(instanceId);
    }

    private void updateImportValidationStatus(UUID recordID, ImportMetadata.RecordValidationStatus status) {
        TransactionTemplate transactionUpdate = new TransactionTemplate(transactionManager);
        transactionUpdate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionUpdate.executeWithoutResult(c -> metadataRepository.updateRecordValidationStatus(recordID, status));
    }

    @Override
    public void updateRecordImportStatus(UUID recordId, ImportMetadata.RecordImportStatus status) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        //todo: should this be version-specific?
        transactionTemplate.executeWithoutResult(c -> metadataRepository.updateRecordImportStatus(recordId, status));
    }

    @Override
    public void fillCollectionsForMetadata(ImportMetadata metadata) {
        List<ImportValidation> validations=importValidationRepository.retrieveValidationsByInstanceId(metadata.getInstanceId());
        log.trace("fillCollectionsForMetadata found {} validations", validations.size());
        metadata.validations.clear();
        metadata.validations.addAll(validations);

        List<KeyValueMapping> mappings= keyValueMappingRepository.findRecordsByRecordId(metadata.getRecordId());
        metadata.keyValueMappings.clear();
        metadata.keyValueMappings.addAll(mappings);
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

    @Override
    public Page page(Pageable pageable) {
        return metadataRepository.findAll(pageable);
    }

    @Override
    public void synchronizeRecord(String entityId, String entityType, String entityContext) {
        log.trace("synchronizeRecord entityId {}", entityId);
        StringBuilder builder = new StringBuilder();
        ObjectNode settings = JsonNodeFactory.instance.objectNode();
        log.trace("getUuidCodeSystem from config: {}", gsrsFactoryConfiguration.getUuidCodeSystem().get(entityContext));
        settings.put("refUuidCodeSystem", gsrsFactoryConfiguration.getUuidCodeSystem().get(entityContext));
        settings.put("refApprovalIdCodeSystem", gsrsFactoryConfiguration.getApprovalIdCodeSystem().get(entityContext));
        log.trace("About to call synchronizeEntity");
        _entityServiceRegistry.get(entityType).synchronizeEntity(entityId, builder::append, settings);
        log.trace("result of synchronizeEntity: {}", builder);
    }

    private String serializeObject(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

    /*
    After a domain entity within the staging area is updated, we rerun validation and matching and store the results
    call this method inside a transaction
     */
    public void propagateUpdate(ImportMetadata importMetadata, String entityJson, String entityType, UUID newInstanceId) throws JsonProcessingException {
        log.trace("starting in propagateUpdate");
        //step 1: deserialize domain object
        Object domainObject = deserializeObject(entityType, entityJson);
        //step 2: validate
        ValidationResponse<T> response = validateRecord(entityType, entityJson);
        log.trace("validated");
        List<ValidationMessage> messages = response.getValidationMessages();
        //step 3a: delete previous validations
        importValidationRepository.deleteByInstanceId(importMetadata.getInstanceId());
        log.trace("cleared out previous validations");
        //step 3b: persist new validations
        persistValidationInfo(response, importMetadata.getVersion(), newInstanceId);
        log.trace("persisted new validations");
        //step 4: calculate matchables
        List<MatchableKeyValueTuple> definitionalValueTuples = getMatchables(domainObject);
        log.trace("calculated matchables");
        definitionalValueTuples.forEach(t -> log.trace("key: {}, value: {}", t.getKey(), t.getValue()));
        //step 5a: remove old matchables
        keyValueMappingRepository.deleteByRecordId(importMetadata.getRecordId());
        log.trace("deleted previous matchables");
        //step 5b: persist new matchables
        persistDefinitionalValues(definitionalValueTuples, newInstanceId, importMetadata.getRecordId(), importMetadata.getEntityClassName());
        log.trace("persisted new matchables");
        //step 6: increment version
        metadataRepository.incrementVersion(importMetadata.getRecordId());
        log.trace("called incrementVersion");
    }


    private StagingAreaEntityService<T> getStagingAreaEntityService(Class objectClass) {
        if(_entityServiceRegistry.containsKey(objectClass.getName())) {
            return _entityServiceRegistry.get(objectClass.getName());
        }
        if( objectClass.getSuperclass() !=null){
            return getStagingAreaEntityService(objectClass.getSuperclass());
        }
        return null;
    }

}
