package gsrs.imports.indexers;

import com.fasterxml.jackson.core.JsonProcessingException;
import gsrs.imports.GsrsImportAdapterFactoryFactory;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.repository.ImportDataRepository;
import gsrs.stagingarea.service.StagingAreaService;
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

    StagingAreaService stagingAreaService = null;

    @Autowired
    ImportDataRepository importDataRepository;

    @Autowired
    private GsrsImportAdapterFactoryFactory gsrsImportAdapterFactoryFactory;

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return ImportMetadata.class;
    }

    public RawDataImportMetadataIndexValueMaker(){
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        if(importMetadata.getImportStatus()== ImportMetadata.RecordImportStatus.imported){
            log.trace("skipping processing of ImportMetadata object that has been imported.");
            //todo: consider removing the substance from the list of things indexed for the staging area
            return;
        }
        if( stagingAreaService == null) {
            try {
                String contextName = importMetadata.getEntityClassName();
                //hack!
                if(contextName.contains(".")) {
                    String[] parts =contextName.split("\\.");
                    contextName = parts[parts.length-1].toLowerCase() + "s";
                }
                log.trace("looking for a staging area service for context {}", contextName);
                stagingAreaService =gsrsImportAdapterFactoryFactory.getStagingAreaService(contextName);
                        //AbstractImportSupportingGsrsEntityController.getStagingAreaServiceForExternal(contextName);
                log.trace("got service {}", stagingAreaService);
            } catch (Exception e) {
                log.error("Error obtaining staging area service", e);
                throw new RuntimeException(e);
            }
        }
        try {
            String objectJson = importDataRepository.retrieveByInstanceID(importMetadata.getInstanceId());
            if( objectJson != null && objectJson.length()>0) {
                Object dataObject= stagingAreaService.deserializeObject(importMetadata.getEntityClassName(), objectJson);
                if(dataObject== null){
                    log.warn("deserialized object is null! (importMetadata.getInstanceId(): {}", importMetadata.getInstanceId());
                    return;
                }
                log.trace("deserialized object of class {}", dataObject.getClass().getName());
                IndexValueMaker rawMaker = realFactory.createIndexValueMakerFor(EntityUtils.EntityWrapper.of(dataObject));
                log.trace("instantiated IndexValueMaker");
                //rawMaker.createIndexableValues(dataObject,consumer);
                //based on suggestion from Tyler P:
                rawMaker.createIndexableValues(dataObject,(iv)->{
                    //don't daisy chain suggestion indexes
                    if(!((IndexableValue)iv).suggest()){
                        consumer.accept(((IndexableValue)iv).suggestable(false));
                    }
                });
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
