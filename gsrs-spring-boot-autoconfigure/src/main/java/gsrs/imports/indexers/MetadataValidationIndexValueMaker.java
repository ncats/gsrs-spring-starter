package gsrs.imports.indexers;

import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.service.StagingAreaService;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

@Slf4j
public class MetadataValidationIndexValueMaker implements IndexValueMaker<ImportMetadata> {
    public final static String IMPORT_METADATA_VALIDATION_TYPE_FACET="Validation Type";
    public final static String IMPORT_METADATA_VALIDATION_MESSAGE_FACET="Validation Message";

    //@Autowired
    StagingAreaService stagingAreaService;

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return ImportMetadata.class;
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        log.trace("In createIndexableValues");
        if(stagingAreaService ==null) {
            try {
                stagingAreaService = AbstractImportSupportingGsrsEntityController.getStagingAreaServiceForExternal("substances");
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                log.error("Error creating staging area service!");
                throw new RuntimeException(e);
            }
        }
        if( importMetadata.getInstanceId()==null) {
            log.warn("importMetadata.getInstanceId() null! ");
            return;
        }
        log.trace("importMetadata.getInstanceId(): {}; importMetadata.getRecordId()", importMetadata.getInstanceId(), importMetadata.getRecordId());
        ValidationResponse validationResponse= stagingAreaService.validateInstance(importMetadata.getInstanceId().toString());
        validationResponse.getValidationMessages().forEach(vm->{
            consumer.accept (IndexableValue.simpleFacetStringValue(IMPORT_METADATA_VALIDATION_TYPE_FACET,
                    String.valueOf(((ValidationMessage)vm).getMessageType())));
            consumer.accept (IndexableValue.simpleFacetStringValue(IMPORT_METADATA_VALIDATION_MESSAGE_FACET,
                    ((ValidationMessage)vm).getMessage()));
        });
    }
}
