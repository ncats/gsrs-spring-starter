package gsrs.imports.indexers;

import gsrs.holdingarea.model.ImportMetadata;
import gsrs.holdingarea.service.HoldingAreaService;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;

@Slf4j
public class MetadataValidationIndexValueMaker implements IndexValueMaker<ImportMetadata> {
    public final static String IMPORT_METADATA_VALIDATION_TYPE_FACET="Validation Type";
    public final static String IMPORT_METADATA_VALIDATION_MESSAGE_FACET="Validation Message";

    @Autowired
    HoldingAreaService holdingAreaService;

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return ImportMetadata.class;
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        log.trace("In createIndexableValues");
        ValidationResponse validationResponse= holdingAreaService.validateInstance(importMetadata.getInstanceId().toString());
        validationResponse.getValidationMessages().forEach(vm->{
            consumer.accept (IndexableValue.simpleFacetStringValue(IMPORT_METADATA_VALIDATION_TYPE_FACET,
                    String.valueOf(((ValidationMessage)vm).getMessageType())));
            consumer.accept (IndexableValue.simpleFacetStringValue(IMPORT_METADATA_VALIDATION_MESSAGE_FACET,
                    ((ValidationMessage)vm).getMessage()));
        });
    }
}
