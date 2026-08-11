package gsrs.dataexchange.imports.indexers;

import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.service.StagingAreaService;
import gsrs.imports.indexers.MetadataValidationIndexValueMaker;
import ix.core.models.Group;
import ix.core.search.text.IndexableValue;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
        Field serviceField = indexValueMaker1.getClass().getDeclaredField("stagingAreaService");
        serviceField.setAccessible(true);
        StagingAreaService stagingAreaService = mock(StagingAreaService.class);

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

            @Override
            public String getMessageId() {
                return "W000000";
            }
        };
        vr.addValidationMessage(vm);
        when(stagingAreaService.validateInstance(metadata.getInstanceId().toString())).thenReturn(vr);
        serviceField.set(indexValueMaker1, stagingAreaService);
        List<IndexableValue> indexedValues = new ArrayList<>();
        indexValueMaker1.createIndexableValues(metadata, indexedValues::add);
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().equals(MetadataValidationIndexValueMaker.IMPORT_METADATA_VALIDATION_TYPE_FACET)
                && (i.value().equals(ValidationMessage.MESSAGE_TYPE.WARNING.toString()))));
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().equals(MetadataValidationIndexValueMaker.IMPORT_METADATA_VALIDATION_MESSAGE_FACET)
                && i.value().equals(tooManyAtomsMessage)));
    }

    @Test
    void testStringCleanup() {
        MetadataValidationIndexValueMaker indexValueMaker = new MetadataValidationIndexValueMaker();
        String input = "Substance PILOCARPINE HYDROCHLORIDE (ID: 2f2b3e85-2cca-41dd-921d-c8eaf6feb376) appears to be a full duplicate";
        String expected = "Substance appears to have a full duplicate";
        String actual = indexValueMaker.cleanValidationMessage(input);
        Assertions.assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("inputData")
    void testStringCleanups(String input, String expected, boolean willMatch) {
        MetadataValidationIndexValueMaker indexValueMaker = new MetadataValidationIndexValueMaker();
        String actual = indexValueMaker.cleanValidationMessage(input);
        Assertions.assertEquals(willMatch, expected.equals(actual));
    }

    private static Stream<Arguments> inputData() {
        return Stream.of(
                Arguments.of("Record PILOCARPINE HYDROCHLORIDE is a potential duplicate", "Record has a potential duplicate", true),
                Arguments.of("Record PILOCARPINE HYDROCHLORIDE is a potential duplicate", "Record might have a potential duplicate", false),
                Arguments.of( "Name (4<I>R</I>)-4-[(3-methylimidazol-4-yl)methyl]oxolan-2-one minimally standardized to (4<i>R</i>)-4-[(3-methylimidazol-4-yl)methyl]oxolan-2-one",
                        "Name was minimally standardized", true),
                Arguments.of( "Name (4<I>R</I>)-4-[(3-methylimidazol-4-yl)methyl]oxolan-2-one minimally standardized to (4<i>R</i>)-4-[(3-methylimidazol-4-yl)methyl]oxolan-2-one",
                        "Name got standardized", false),
                Arguments.of("Substances should have exactly one (1) display name, Default to using:3-ethyl-4-[(3-methyl-1H-imidazol-3-ium-4-yl)methyl]tetrahydrofuran-2-one",
                        "Display name was selected automatically", true),
                Arguments.of("Substances should have exactly one (1) display name, Default to using:3-ethyl-4-[(3-methyl-1H-imidazol-3-ium-4-yl)methyl]tetrahydrofuran-2-one",
                        "name selected automatically", false),
                Arguments.of("Each fragment should be present as a separate record in the database. Please register: [#6][#6]1[#6]*([#6][#6][#6]1)[#6][#6][#6]#[#6][#6]1[#6][#6][#6]([#6][#6]1)[#6]#[#6][#6][#6]*1[#6][#6]([#6][#6][#6]1)[#6]",
                        "Substance contains a fragment that has not been registered as an individual record", true),
                Arguments.of("Name '2-[2-(2-Methoxyethoxy)ethoxy]ethyl 3-methylbutanoate' collides (possible duplicate) with existing name for substance: ",
                        "Duplicate name", true),
                Arguments.of("Name '3-(2-Methoxyethyl) 5-(1-methylethyl) 1,4-dihydro-2,6-dimethyl-4-(4-nitrophenyl)-3,5-pyridinedicarboxylate' collides (possible duplicate) with existing name for substance: ",
                        "Duplicate name", true),
                Arguments.of("Name '(Z)-2-Methoxyethyl 2-[(3-nitrophenyl)methylene]-3-oxobutanoate' collides (possible duplicate) with existing name for substance: ",
                        "Duplicate name", true)
        );
    }
}
