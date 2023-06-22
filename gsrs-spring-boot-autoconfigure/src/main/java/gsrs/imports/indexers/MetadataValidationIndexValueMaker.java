package gsrs.imports.indexers;

import gsrs.imports.GsrsImportAdapterFactoryFactory;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.model.ImportValidation;
import gsrs.stagingarea.service.StagingAreaService;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class MetadataValidationIndexValueMaker implements IndexValueMaker<ImportMetadata> {
    public final static String IMPORT_METADATA_VALIDATION_TYPE_FACET="Validation Type";
    public final static String IMPORT_METADATA_VALIDATION_MESSAGE_FACET="Validation Message";

    //@Autowired
    StagingAreaService stagingAreaService;

    @Autowired
    private GsrsImportAdapterFactoryFactory gsrsImportAdapterFactoryFactory;

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return ImportMetadata.class;
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        log.trace("In createIndexableValues");
        if(stagingAreaService ==null) {
            try {
                String contextName = importMetadata.getEntityClassName();
                //hack!
                if(contextName.contains(".")) {
                    String[] parts =contextName.split("\\.");
                    contextName = parts[parts.length-1].toLowerCase() + "s";
                }
                stagingAreaService =gsrsImportAdapterFactoryFactory.getStagingAreaService(contextName);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                log.error("Error creating staging area service!");
                throw new RuntimeException(e);
            }
        }
        if( importMetadata.getInstanceId()==null) {
            log.warn("importMetadata.getInstanceId() null! ");
            return;
        }
        log.trace("importMetadata.getInstanceId(): {}; importMetadata.getRecordId(): {}", importMetadata.getInstanceId(), importMetadata.getRecordId());
        List<ImportValidation> validations =stagingAreaService.retrieveValidationForInstance(importMetadata.getInstanceId());
        if(validations == null || validations.isEmpty()) {
            log.info("No validations found; going to validate before computing facets");
            ValidationResponse validationResponse = stagingAreaService.validateInstance(importMetadata.getInstanceId().toString());
            validationResponse.getValidationMessages().forEach(vm -> {
                consumer.accept(IndexableValue.simpleFacetStringValue(IMPORT_METADATA_VALIDATION_TYPE_FACET,
                        String.valueOf(((ValidationMessage) vm).getMessageType())));
                consumer.accept(IndexableValue.simpleFacetStringValue(IMPORT_METADATA_VALIDATION_MESSAGE_FACET,
                        ((ValidationMessage) vm).getMessage()));
            });
            return;
        }
        validations.forEach(v->{
            consumer.accept (IndexableValue.simpleFacetStringValue(IMPORT_METADATA_VALIDATION_TYPE_FACET,
                    String.valueOf((v.getValidationType()))));
            consumer.accept (IndexableValue.simpleFacetStringValue(IMPORT_METADATA_VALIDATION_MESSAGE_FACET,
                    (v.getValidationMessage())));
        });
    }
}
