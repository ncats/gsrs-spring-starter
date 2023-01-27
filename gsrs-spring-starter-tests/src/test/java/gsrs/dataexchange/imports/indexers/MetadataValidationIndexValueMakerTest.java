package gsrs.dataexchange.imports.indexers;

import gsrs.holdingarea.model.ImportMetadata;
import gsrs.holdingarea.service.HoldingAreaService;
import gsrs.imports.indexers.MetadataValidationIndexValueMaker;
import ix.core.models.Group;
import ix.core.search.text.IndexableValue;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataValidationIndexValueMakerTest {
    @Test
    public void createIndexableValuesTest() throws NoSuchFieldException, IllegalAccessException {
        ImportMetadata metadata = new ImportMetadata();
        metadata.setAccess( Collections.singleton(new Group("protected")));
        metadata.setReason("test");
        String sourceName = "Unique Data Source";
        metadata.setSourceName(sourceName);
        metadata.setInstanceId(UUID.randomUUID());
        metadata.setEntityClassName("ix.ginas.models.v1.Substance");
        MetadataValidationIndexValueMaker indexValueMaker1 = new MetadataValidationIndexValueMaker();
        Field serviceField = indexValueMaker1.getClass().getDeclaredField("holdingAreaService");
        serviceField.setAccessible(true);
        HoldingAreaService holdingAreaService = mock(HoldingAreaService.class);

        String tooManyAtomsMessage = "Warning! The structure contains too many atoms";
        ValidationResponse vr = new ValidationResponse();
        ValidationMessage vm = new ValidationMessage() {
            @Override
            public String getMessage() {
                return tooManyAtomsMessage;
            }

            @Override
            public MESSAGE_TYPE getMessageType() {
                return MESSAGE_TYPE.WARNING;
            }
        };
        vr.addValidationMessage(vm);
        when(holdingAreaService.validateInstance(metadata.getInstanceId().toString())).thenReturn(vr);
        serviceField.set(indexValueMaker1, holdingAreaService);
        List<IndexableValue> indexedValues = new ArrayList<>();
        indexValueMaker1.createIndexableValues(metadata, indexedValues::add);
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().equals(MetadataValidationIndexValueMaker.IMPORT_METADATA_VALIDATION_TYPE_FACET)
                && (i.value().equals(ValidationMessage.MESSAGE_TYPE.WARNING.toString()))));
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().equals(MetadataValidationIndexValueMaker.IMPORT_METADATA_VALIDATION_MESSAGE_FACET)
                && i.value().equals(tooManyAtomsMessage)));
    }
}
