package gsrs.holdingArea.service;

import gsrs.holdingArea.model.CreateRecordParameters;
import gsrs.holdingArea.model.ImportMetadata;
import gsrs.holdingArea.model.MatchableKeyValueTuple;
import gsrs.holdingArea.model.MatchedRecordSummary;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexerFactory;
import ix.core.validator.ValidationMessage;

import java.util.List;

public interface HoldingAreaService {
    String createRecord(CreateRecordParameters parameters);

    String updateRecord(String recordId, String jsonData);

    ImportMetadata retrieveRecord(String recordId, int version);

    void deleteRecord(String recordId, int version);

    SearchResult findRecords(SearchRequest searchRequest);

    List<ValidationMessage> validateRecord(String json);

    <T> List<gsrs.holdingArea.model.MatchableKeyValueTuple> calculateMatchables(T domainObject);

    MatchedRecordSummary findMatches(List<gsrs.holdingArea.model.MatchableKeyValueTuple> recordMatchables);

}
