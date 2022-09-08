package gsrs.holdingarea.service;

import gsrs.holdingarea.model.*;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexer;
import ix.core.validator.ValidationResponse;

import java.util.List;
import java.util.UUID;

public interface HoldingAreaService {
    String createRecord(ImportRecordParameters parameters);

    String updateRecord(String recordId, String jsonData);

    ImportMetadata retrieveRecord(String recordId, int version);

    List<UUID> getInstancesForRecord(String recordId);

    List<ImportData> getDataForRecord(String recordId);

    String getInstanceData(String instanceId);

    void deleteRecord(String recordId, int version);

    <T> SearchResult findRecords(SearchRequest searchRequest, Class<T> cls);

    <T> ValidationResponse<T> validateRecord(String entityClass, String json);

    <T> List<gsrs.holdingarea.model.MatchableKeyValueTuple> calculateMatchables(T domainObject);

    MatchedRecordSummary findMatches(String entityClass, List<gsrs.holdingarea.model.MatchableKeyValueTuple> recordMatchables) throws ClassNotFoundException;

    void setIndexer(TextIndexer indexer);

    <T> void registerEntityService(HoldingAreaEntityService<T> service);

    MatchedRecordSummary findMatchesForJson(String qualifiedEntityType, String entityJson);

    <T> String persistEntity(String instanceId);
}
