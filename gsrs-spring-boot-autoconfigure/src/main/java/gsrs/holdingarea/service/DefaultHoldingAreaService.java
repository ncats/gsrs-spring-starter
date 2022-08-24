package gsrs.holdingarea.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.holdingarea.model.*;
import gsrs.holdingarea.repository.*;
import gsrs.springUtils.AutowireHelper;
import gsrs.validator.GsrsValidatorFactory;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import ix.core.validator.*;
import ix.ginas.utils.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DefaultHoldingAreaService implements HoldingAreaService {

    //TODO: DISCUSS with team whether this is the best way to go.
    public static String IMPORT_FAILURE ="ERROR";
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

    private ValidatorFactory validatorFactory;

    private TextIndexer indexer;

    private final String context;

    private MatchableCalculationConfig matchableCalculationConfig;

    public final static String CURRENT_SOURCE ="Holding Area";

    private Map<String, HoldingAreaEntityService> _entityServiceRegistry = new HashMap<>();

    public DefaultHoldingAreaService(String context) {

        this.context = Objects.requireNonNull(context, "context can not be null");
        if(tif!=null) {
            indexer=tif.getDefaultInstance();
            log.trace("got indexer from tif.getDefaultInstance()");
        } else {
            try {
                log.trace("going to create indexerFactory");
                TextIndexerFactory indexerFactory = new TextIndexerFactory();
                AutowireHelper.getInstance().autowireAndProxy( indexerFactory);
                indexer = indexerFactory.getDefaultInstance();
                log.trace("got indexer from indexerFactory.getDefaultInstance(): " + indexer);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }


    @Override
    public String createRecord(ImportRecordParameters parameters) {
        Objects.requireNonNull(indexer, "need a text indexer!");
        ImportData data = new ImportData();
        data.setData(parameters.getJsonData());
        UUID recordId = UUID.randomUUID();
        UUID instanceId= UUID.randomUUID();
        data.setRecordId(recordId);
        data.setVersion(1);
        data.setInstanceId(instanceId);
        data.setEntityClassName(parameters.getEntityClassName());
        Objects.requireNonNull(importDataRepository,"importDataRepository is required");
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
        /*try {
            indexer.add(wrapper);
        } catch (IOException e) {
            log.error("Error indexing import metadata to index", e);
        }*/
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
        if(domainObject == null) {
            log.warn("null domainObject!");
            return recordId.toString();
        }

        ValidationResponse response= _entityServiceRegistry.get(parameters.getEntityClassName()).validate(domainObject);
        ImportMetadata.RecordValidationStatus overallStatus = ImportMetadata.RecordValidationStatus.unparseable;
        if( response!=null) {
            persistValidationInfo(response, 1, instanceId);
            overallStatus = getOverallValidationStatus(response);
        }

        log.trace("overallStatus: " + overallStatus);
        updateImportValidationStatus(instanceId, overallStatus);

        //TODO: extend this to other types of objects
        List<MatchableKeyValueTuple> definitionalValueTuples = getMatchables(domainObject);
        definitionalValueTuples.forEach(t -> log.trace("key: {}, value: {}", t.getKey(), t.getValue()));
        persistDefinitionalValues(definitionalValueTuples, instanceId);
        updateRecordImportStatus(instanceId, ImportMetadata.RecordImportStatus.staged);

        //event driven: each step in process sends an event (pub/sub) look in ... indexing
        //  validation, when done would trigger the next event
        //  event manager type of thing
        // passively: daemon running in background looks for records with a given status and then performs
        // the next step
        // will
        //todo: run duplicate check
        try {
            MatchedRecordSummary summary= findMatches(domainObject.getClass().getName(), definitionalValueTuples);
            log.trace("Matches: ");
            summary.getMatches().forEach(m->{
                log.trace("One match:");
                m.getMatchingRecords().forEach(r->log.trace("   source: {} record Id: {}", r.getSourceName(),r.getRecordId()));
            });
        } catch (ClassNotFoundException e) {
            log.error("Error looking for matches", e);
        }

        return saved.getRecordId().toString();
    }

    @Override
    public String updateRecord(String recordId, String jsonData) {
        return null;
    }

    @Override
    public ImportMetadata retrieveRecord(String recordId, int version) {
        return metadataRepository.retrieveByIDAndVersion(UUID.fromString(recordId), version);
    }

    @Override
    public void deleteRecord(String recordId, int version) {
        importDataRepository.deleteById(UUID.fromString(recordId));
    }

    @Override
    public <T> void registerEntityService(HoldingAreaEntityService<T> service){
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
    public ValidationResponse validateRecord(String entityClass, String json) {

        try {
            Object object = deserializeObject (entityClass, json);
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
            }
            catch (Exception e) {
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
            List<MatchedKeyValue.MatchingRecordReference> matches=mappings.stream()
                    .map(ma-> MatchedKeyValue.MatchingRecordReference.builder()
                            .recordId(EntityUtils.Key.of(objectClass, ma.getInstanceId() ))
                            .sourceName(CURRENT_SOURCE)
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

    public String getContext() {
        return context;
    }

    public void setMatchableCalculationConfig(MatchableCalculationConfig matchableCalculationConfig) {
        this.matchableCalculationConfig = matchableCalculationConfig;
    }

    private <T> List<MatchableKeyValueTuple> getMatchables(T entity) {
        String lookupClass =entity.getClass().getName();
        log.trace("getMatchables looking for extractor for class {}", lookupClass);
        if( !_entityServiceRegistry.containsKey(lookupClass)) {
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
    private void persistDefinitionalValues(List<MatchableKeyValueTuple> definitionalValues, UUID instanceId) {

        definitionalValues.forEach(kv -> {
            KeyValueMapping mapping = new KeyValueMapping();
            mapping.setKey(kv.getKey());
            mapping.setValue(kv.getValue());
            mapping.setInstanceId(instanceId);
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

    private <T> ValidatorCallback createCallbackFor(T entity, ValidationResponse<T> response) {
        return new ValidatorCallback() {
            @Override
            public void addMessage(ValidationMessage message) {
                response.addValidationMessage(message);
            }

            @Override
            public void setInvalid() {
                response.setValid(false);
            }

            @Override
            public void setValid() {
                response.setValid(true);
            }

            @Override
            public void haltProcessing() {

            }

            @Override
            public void addMessage(ValidationMessage message, Runnable appyAction) {
                response.addValidationMessage(message);
                appyAction.run();
            }

            @Override
            public void complete() {
                if (response.hasError()) {
                    response.setValid(false);
                }
            }
        };
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
        TransactionTemplate transactionUpdate= new TransactionTemplate(transactionManager);
        transactionUpdate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionUpdate.executeWithoutResult(c-> metadataRepository.updateRecordValidationStatus(instanceId, status));
    }

    private void updateRecordImportStatus(UUID instanceId, ImportMetadata.RecordImportStatus status) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(c->metadataRepository.updateRecordImportStatus(instanceId, status));
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

    private Object deserializeObject(String entityClassName, String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        if(_entityServiceRegistry.containsKey(entityClassName)) {
            return _entityServiceRegistry.get(entityClassName).parse(node);
        }
        else {
            log.error("No entity service for class {}", entityClassName);
            return null;
        }
    }

    private ValidationResponse  handleValidation(String entityClass, Object object) {
        return _entityServiceRegistry.get(entityClass).validate(object);
    }
}
