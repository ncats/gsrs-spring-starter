package gsrs.holdingArea.repository;

import gsrs.holdingArea.model.DefinitionalValueTuple;
import ix.core.validator.GinasProcessingMessage;

import java.sql.Clob;
import java.util.List;

/*
Todo: resolve circular dependency!
 */
public interface HoldingAreaService {
    public String createRecord(String jsonData, String source, byte[] rawData, String formatType);

    public String updateRecord(String recordId, Clob jsonData);

    public Clob retrieveRecord(String recordId, int version, String view);

    public void deleteRecord(String recordId, int version);

    public List<String> findRecords(String query);

    public List<DefinitionalValueTuple> calculateDefinitions(Clob json);

    public List<GinasProcessingMessage> validateRecord(Clob json);
}
