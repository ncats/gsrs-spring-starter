package gsrs.holdingArea.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.holdingArea.model.*;
import gsrs.holdingArea.repository.*;
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
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
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
        Objects.requireNonNull(importDataRepository,"importDataRepository is required");
        ImportData saved = importDataRepository.saveAndFlush(data);

        ImportMetadata metadata = new ImportMetadata();
        metadata.setRecordId(recordId);
        metadata.setInstanceId(instanceId);
        metadata.setEntityClassName(parameters.getEntityClass().getName());
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
        } catch (IOException e) {
            log.error("Error indexing import metadata to index", e);
        }
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
            domainObject = deserializeObject(parameters.getEntityClass().getName(), parameters.getJsonData());
        } catch (JsonProcessingException e) {
            log.error("Error deserializing imported object.", e);
            return IMPORT_FAILURE;
        }
        ValidationResponse response= _entityServiceRegistry.get(parameters.getEntityClass()).validate(domainObject);
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
        metadataRepository.updateRecordImportStatus(instanceId, ImportMetadata.RecordImportStatus.staged);

        //event driven: each step in process sends an event (pub/sub) look in ... indexing
        //  validation, when done would trigger the next event
        //  event manager type of thing
        // passively: daemon running in background looks for records with a given status and then performs
        // the next step
        // will
        //todo: run duplicate check

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



    public <T> void registerEntityService(HoldingAreaEntityService<T> service){
        _entityServiceRegistry.put(service.getEntityClass().toString(), service);
    }


    @Override
    public <T> List<MatchableKeyValueTuple> calculateMatchables(T object) {
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
        return _entityServiceRegistry.get(entity.getClass().getName()).extractKVM(entity);
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
            try {
                indexer.add(wrapper);
            } catch (IOException e) {
                log.error("Error indexing import metadata to index", e);
            }
        });
    }

    /*private <T> List<T> getSearchList(SearchRequest sr, Class<T> cls) {
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        List<T> foundObjects = transactionSearch.execute(ts -> {
            try {
                SearchResult sresult = importMetadataLegacySearchService.search(sr.getQuery(), sr.getOptions());
                List<T> first = sresult.getMatches();
                log.trace("first size: {}", first.size());
                if (!first.isEmpty()) {
                    log.trace("first item:" + first.get(0).getClass().getName());
                } else {
                    log.trace("first empty");
                }
                return first.stream()
                        //force fetching
                        .peek(ss -> EntityUtils.EntityWrapper.of(ss).toInternalJson())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new RuntimeException(e);

            }
        });
        return foundObjects;
    }*/


    /*public byte[] calculateDefinitionalHash(Substance substance) {
        DefHashCalcRequirements defHashCalcRequirements = new DefHashCalcRequirements(definitionalElementFactory, substanceSearchService, transactionManager);
        DefinitionalElements definitionalElements = defHashCalcRequirements.getDefinitionalElementFactory().computeDefinitionalElementsFor(substance);
        return definitionalElements.getDefinitionalHash();
    }*/

    /*private List<gsrs.holdingArea.model.MatchableKeyValueTupleExtractor<Substance>> getExtractors(Class T) {
        List<gsrs.holdingArea.model.MatchableKeyValueTupleExtractor<Substance>> extractors = new ArrayList<>();
        //todo: get from config
        AllNamesMatchableExtractor<Substance> namesMatchableExtractor = new AllNamesMatchableExtractor<>();
        extractors.add(namesMatchableExtractor);
        ApprovalIdMatchableExtractor approvalIdMatchableExtractor = new ApprovalIdMatchableExtractor();
        extractors.add(approvalIdMatchableExtractor);
        CASNumberMatchableExtractor casNumberMatchableExtractor = new CASNumberMatchableExtractor();
        extractors.add(casNumberMatchableExtractor);
        SelectedCodesMatchableExtractor selectedCodesMatchableExtractor = new SelectedCodesMatchableExtractor();
        extractors.add(selectedCodesMatchableExtractor);
        UUIDMatchableExtractor uuidMatchableExtractor = new UUIDMatchableExtractor();
        extractors.add(uuidMatchableExtractor);
        return extractors;
    }*/

    /*
    TODO: Make this validate in a generic way!
     */
    /*private <T> ValidationResponse<T> validateEntity(T newEntity) {
        Objects.requireNonNull(substanceEntityService, "Must have a SubstanceEntityService!");
        try {
            ValidationResponse response = substanceEntityService.validateEntity(((Substance) newEntity).toFullJsonNode());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }*/

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
            gsrs.holdingArea.model.ImportValidation.ImportValidationType type = gsrs.holdingArea.model.ImportValidation.ImportValidationType.info;
            if (m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR) {
                type = gsrs.holdingArea.model.ImportValidation.ImportValidationType.error;
            } else if (m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING) {
                type = gsrs.holdingArea.model.ImportValidation.ImportValidationType.warning;
            }
            UUID validationId = UUID.randomUUID();
            gsrs.holdingArea.model.ImportValidation validation = gsrs.holdingArea.model.ImportValidation.builder()
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
        metadataRepository.updateRecordValidationStatus(instanceId, status);
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

    //for unit tests
    public void setIndexer(TextIndexer indexer) {
        this.indexer = indexer;
    }

    private Object deserializeObject(String entityClassName, String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entityClassName);
        Object domainObject = _entityServiceRegistry.get(entityClassName).parse(node);
        return domainObject;
    }

    private ValidationResponse  handleValidation(String entityClass, Object object) {
        return _entityServiceRegistry.get(entityClass).validate(object);
    }

}
