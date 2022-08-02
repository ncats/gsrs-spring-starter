package gsrs.holdingArea.service;

import gsrs.holdingArea.model.ImportRecordParameters;
import gsrs.holdingArea.model.ImportMetadata;
import gsrs.holdingArea.model.MatchedRecordSummary;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.validator.ValidationResponse;

import java.util.List;

public interface HoldingAreaService {
    String createRecord(ImportRecordParameters parameters);

    String updateRecord(String recordId, String jsonData);

    ImportMetadata retrieveRecord(String recordId, int version);

    void deleteRecord(String recordId, int version);

    public <T> SearchResult findRecords(SearchRequest searchRequest, Class<T> cls);

    ValidationResponse validateRecord(String entityClass, String json);

    <T> List<gsrs.holdingArea.model.MatchableKeyValueTuple> calculateMatchables(T domainObject);

    MatchedRecordSummary findMatches(String entityClass, List<gsrs.holdingArea.model.MatchableKeyValueTuple> recordMatchables) throws ClassNotFoundException;

}
