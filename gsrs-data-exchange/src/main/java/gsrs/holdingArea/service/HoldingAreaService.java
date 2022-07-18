package gsrs.holdingArea.service;

import gsrs.holdingArea.model.CreateRecordParameters;
import gsrs.holdingArea.model.MatchedRecordSummary;
import ix.core.validator.ValidationMessage;
import ix.ginas.models.GinasCommonData;

import java.util.List;

public interface HoldingAreaService {
    String createRecord(CreateRecordParameters parameters);

    String updateRecord(String recordId, String jsonData);

    String retrieveRecord(String recordId, int version, String view);

    void deleteRecord(String recordId, int version);

    <T> List<T> findRecords(String query, Class<T> cls);

    List<gsrs.holdingArea.model.MatchableKeyValueTuple> calculateDefinitions(String json);

    byte[] getDefinitionalHash(String json);

    List<ValidationMessage> validateRecord(String json);

    List<gsrs.holdingArea.model.MatchableKeyValueTuple> calculateMatchables(GinasCommonData substance);

    MatchedRecordSummary findMatches(List<gsrs.holdingArea.model.MatchableKeyValueTuple> recordMatchables);

}
