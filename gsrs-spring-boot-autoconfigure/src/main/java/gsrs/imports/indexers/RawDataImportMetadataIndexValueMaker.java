package gsrs.imports.indexers;

import com.fasterxml.jackson.core.JsonProcessingException;
import gsrs.holdingarea.model.ImportMetadata;
import gsrs.holdingarea.repository.ImportDataRepository;
import gsrs.holdingarea.service.HoldingAreaService;
import gsrs.indexer.IndexValueMakerFactory;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.util.EntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;

@Slf4j
public class RawDataImportMetadataIndexValueMaker implements IndexValueMaker<ImportMetadata> {

    //May not work this way, need to check
    @Autowired
    IndexValueMakerFactory realFactory;

    @Autowired
    HoldingAreaService holdingAreaService;

    @Autowired
    ImportDataRepository importDataRepository;

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return ImportMetadata.class;
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        try {
            String objectJson = importDataRepository.retrieveByInstanceID(importMetadata.getInstanceId());
            if( objectJson != null && objectJson.length()>0) {
                Object dataObject= holdingAreaService.deserializeObject(importMetadata.getEntityClassName(), objectJson);
                log.trace("deserialized object");
                IndexValueMaker rawMaker = realFactory.createIndexValueMakerFor(EntityUtils.EntityWrapper.of(dataObject));
                log.trace("instantiated rawMaker");
                rawMaker.createIndexableValues(dataObject,consumer);
                log.trace("called createIndexableValues");
            } else {
                log.info("No import data found for instance ID: {}", importMetadata.getInstanceId() != null ? importMetadata.getInstanceId().toString()
                        : "[null]");
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

}
