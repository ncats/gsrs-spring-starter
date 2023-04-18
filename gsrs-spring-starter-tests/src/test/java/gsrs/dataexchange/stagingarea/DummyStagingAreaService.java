package gsrs.dataexchange.stagingarea;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.service.GsrsEntityService;
import gsrs.stagingarea.model.*;
import gsrs.stagingarea.service.StagingAreaEntityService;
import gsrs.stagingarea.service.StagingAreaService;
import ix.core.models.Mesh;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexer;
import ix.core.validator.ValidationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public class DummyStagingAreaService implements StagingAreaService {
    @Override
    public String createRecord(ImportRecordParameters parameters) {
        Mesh dataObject = new Mesh();
        if( parameters.getSettings()!=null && parameters.getSettings() instanceof ObjectNode) {
            ObjectNode dataNode = (ObjectNode) parameters.getSettings();
            if(dataNode.hasNonNull("heading")) {
                dataObject.heading=dataNode.get("heading").asText();
            }

        }
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.writeValueAsString(dataObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String updateRecord(String recordId, String jsonData) {
        return null;
    }

    @Override
    public ImportMetadata getImportMetaData(String recordId, int version) {
        return null;
    }

    @Override
    public ImportMetadata getImportMetaData(String instanceId) {
        return null;
    }

    @Override
    public List<UUID> getInstancesForRecord(String recordId) {
        return null;
    }

    @Override
    public List<ImportData> getImportData(String recordId) {
        return null;
    }

    @Override
    public String getInstanceData(String instanceId) {
        return null;
    }

    @Override
    public ImportData getImportDataByInstanceIdOrRecordId(String id, int version) {
        return null;
    }

    @Override
    public void deleteRecord(String recordId, int version) {

    }

    @Override
    public <T> SearchResult findRecords(SearchRequest searchRequest, Class<T> cls) {
        return null;
    }

    @Override
    public <T> ValidationResponse<T> validateRecord(String entityClass, String json) {
        return null;
    }

    @Override
    public <T> ValidationResponse<T> validateInstance(String instanceId) {
        return null;
    }

    @Override
    public List<ImportValidation> retrieveValidationForInstance(UUID instanceId) {
        return null;
    }

    @Override
    public <T> List<MatchableKeyValueTuple> calculateMatchables(T domainObject) {
        return null;
    }

    @Override
    public MatchedRecordSummary findMatches(String entityClass, List<MatchableKeyValueTuple> recordMatchables, String startingRecordId) throws ClassNotFoundException {
//        MatchedRecordSummary matches = new MatchedRecordSummary();
//        matches.setQuery(recordMatchables);
//        MatchedKeyValue oneMatch= new MatchedKeyValue();
//        //oneMatch.set
//        //matches.setMatches();
//        recordMatchables.forEach(r-> {
//
//            r.getKey()
//        });
//        matches.set
        return null;
    }

    @Override
    public MatchedRecordSummary findMatches(ImportMetadata importMetadata) throws ClassNotFoundException, JsonProcessingException {
        return null;
    }

    @Override
    public void setIndexer(TextIndexer indexer) {

    }

    @Override
    public <T> void registerEntityService(StagingAreaEntityService<T> service) {

    }

    @Override
    public MatchedRecordSummary findMatchesForJson(String qualifiedEntityType, String entityJson, String startingRecordId) {
        return null;
    }

    @Override
    public <T> T retrieveEntity(String entityType, String entityId) {
        return null;
    }

    @Override
    public <T> T deserializeObject(String entityClassName, String objectJson) throws JsonProcessingException {
        return null;
    }

    @Override
    public <T> GsrsEntityService.ProcessResult<T> saveEntity(String entityType, T entity, boolean brandNew) {
        return null;
    }

    @Override
    public void updateRecordImportStatus(UUID recordId, ImportMetadata.RecordImportStatus status) {

    }

    @Override
    public void fillCollectionsForMetadata(ImportMetadata metadata) {

    }

    @Override
    public Page page(Pageable pageable) {
        return null;
    }
}
