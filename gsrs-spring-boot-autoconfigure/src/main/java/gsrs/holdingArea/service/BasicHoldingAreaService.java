package gsrs.holdingArea.service;

import gsrs.holdingArea.model.*;
import gsrs.holdingArea.repository.ImportDataRepository;
import gsrs.holdingArea.repository.ImportMetadataRepository;
import ix.core.search.text.TextIndexerFactory;
import ix.core.validator.ValidationMessage;
import ix.ginas.models.GinasCommonData;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

public class BasicHoldingAreaService /* implements HoldingAreaService*/ {

    @Autowired
    ImportMetadataRepository metadataRepository;

    @Autowired
    ImportDataRepository dataRepository;

    public String createRecord(String jsonData, String source, byte[] rawData, String formatType) {
        //todo: run duplicate check
        ImportData data = new ImportData();
        data.setData(jsonData);
        UUID recordId = UUID.randomUUID();
        data.setRecordId(recordId);
        data.setVersion(1);
        ImportData saved= dataRepository.saveAndFlush(data);

        ImportMetadata metadata = new ImportMetadata();
        metadata.setEntityClass(ImportData.class.getName());
        metadata.setRecordId(recordId);
        metadata.setImportStatus(ImportMetadata.RecordImportStatus.imported);
        metadata.setProcessStatus(ImportMetadata.RecordProcessStatus.loaded);

        return saved.getRecordId().toString();
    }


    //@Override
    public String createRecord(CreateRecordParameters parameters) {
        return null;
    }

    //@Override
    public String updateRecord(String recordId, String jsonData) {
        return null;
    }

    //@Override
    public String retrieveRecord(String recordId, int version, String view) {
        return null;
    }

    //@Override
    public void deleteRecord(String recordId, int version) {

    }

    //@Override
    public <T> List<T> findRecords(String query, Class<T> cls) {
        return null;
    }

    //@Override
    public List<MatchableKeyValueTuple> calculateDefinitions(String json) {
        return null;
    }

    //@Override
    public byte[] getDefinitionalHash(String json) {
        return new byte[0];
    }

    //@Override
    public List<ValidationMessage> validateRecord(String json) {
        return null;
    }

    //@Override
    public List<MatchableKeyValueTuple> calculateMatchables(Object object) {
        return null;
    }

    //@Override
    public MatchedRecordSummary findMatches(List<MatchableKeyValueTuple> recordMatchables) {
        return null;
    }

    //@Override
    public void setTextIndexerFactory(TextIndexerFactory textIndexerFactory){

    }
}
