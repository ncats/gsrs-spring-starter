package gsrs.holdingarea.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import gsrs.holdingarea.model.*;
import gsrs.service.GsrsEntityService;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexer;
import ix.core.validator.ValidationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface HoldingAreaService {
    String createRecord(ImportRecordParameters parameters);

    String updateRecord(String recordId, String jsonData);

    ImportMetadata getImportMetaData(String recordId, int version);

    ImportMetadata getImportMetaData(String instanceId);

    List<UUID> getInstancesForRecord(String recordId);

    List<ImportData> getImportData(String recordId);

    String getInstanceData(String instanceId);

    ImportData getImportDataByInstanceIdOrRecordId(String id, int version);

    void deleteRecord(String recordId, int version);

    <T> SearchResult findRecords(SearchRequest searchRequest, Class<T> cls);

    <T> ValidationResponse<T> validateRecord(String entityClass, String json);

    <T> ValidationResponse<T> validateInstance(String instanceId);

    <T> List<gsrs.holdingarea.model.MatchableKeyValueTuple> calculateMatchables(T domainObject);

    MatchedRecordSummary findMatches(String entityClass, List<gsrs.holdingarea.model.MatchableKeyValueTuple> recordMatchables) throws ClassNotFoundException;

    void setIndexer(TextIndexer indexer);

    <T> void registerEntityService(HoldingAreaEntityService<T> service);

    MatchedRecordSummary findMatchesForJson(String qualifiedEntityType, String entityJson);

    <T> String persistEntity(String instanceId);

    <T> T retrieveEntity(String entityType, String entityId);

    <T> T deserializeObject(String entityClassName, String objectJson) throws JsonProcessingException;

    <T> GsrsEntityService.ProcessResult<T> persistPermanentEntity(String entityType, T entity);

    void updateRecordImportStatus(UUID instanceId, ImportMetadata.RecordImportStatus status);

    void fillCollectionsForMetadata(ImportMetadata metadata);

    public Page page(Pageable pageable);
}
