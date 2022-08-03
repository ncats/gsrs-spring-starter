package gsrs.holdingarea.service;

import gsrs.holdingarea.model.ImportRecordParameters;
import gsrs.holdingarea.model.ImportMetadata;
import gsrs.holdingarea.model.MatchedRecordSummary;
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

    <T> List<gsrs.holdingarea.model.MatchableKeyValueTuple> calculateMatchables(T domainObject);

    MatchedRecordSummary findMatches(String entityClass, List<gsrs.holdingarea.model.MatchableKeyValueTuple> recordMatchables) throws ClassNotFoundException;

}
