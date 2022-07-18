package gsrs.holdingArea.repository;

import gsrs.holdingArea.model.DefinitionalValueTuple;
import gsrs.holdingArea.model.ImportData;
import gsrs.holdingArea.model.ImportMetadata;
import ix.core.validator.GinasProcessingMessage;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Clob;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DefaultHoldingAreaService implements HoldingAreaService {

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

    @Override
    public String updateRecord(String recordId, Clob jsonData) {
        return null;
    }

    @Override
    public Clob retrieveRecord(String recordId, int version, String view) {
        return null;
    }

    @Override
    public void deleteRecord(String recordId, int version) {

    }

    @Override
    public List<String> findRecords(String query) {

        List<String> records = new ArrayList<>();
        //dataRepository.f

        return records;
    }

    @Override
    public List<DefinitionalValueTuple> calculateDefinitions(Clob json) {
        return null;
    }

    @Override
    public List<GinasProcessingMessage> validateRecord(Clob json) {
        return null;
    }
}
