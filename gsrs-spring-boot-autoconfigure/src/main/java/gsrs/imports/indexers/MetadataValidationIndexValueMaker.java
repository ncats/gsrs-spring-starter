package gsrs.imports.indexers;

import gsrs.config.EntityContextLookup;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

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

    private List<ValidationMessageSubstitution> substitutions;

    public MetadataValidationIndexValueMaker() {
        substitutions = Arrays.asList(
                ValidationMessageSubstitution.of("Substance .* appears to be a full duplicate",
                        "Substance appears to have a full duplicate"),
                ValidationMessageSubstitution.of("Record .* is a potential duplicate",
                        "Record has a potential duplicate"),
                ValidationMessageSubstitution.of("Name .* minimally standardized to .*",
                        "Name was minimally standardized"),
                ValidationMessageSubstitution.of("Substances should have exactly one \\(1\\) display name.*",
                        "Display name was selected automatically"),
                ValidationMessageSubstitution.of("Each fragment should be present as a separate record in the database. Please register:.*",
                        "Substance contains a fragment that has not been registered as an individual record"),
                ValidationMessageSubstitution.of("Substance .* is a possible duplicate", "Record has a possible duplicate"),
                ValidationMessageSubstitution.of("This fragment is present as a separate record in the database but in a different form. Please register: .* as an individual substance",
                        "Substance contains a fragment that has not been registered as an individual record in its current form"),
                ValidationMessageSubstitution.of("Name .* collides \\(possible duplicate\\) with existing name for substance.*", "Duplicate name"),
                ValidationMessageSubstitution.of("Structure is not charged balanced, net charge of:.*", "Structure is not charged balanced"),
                ValidationMessageSubstitution.of("Substance may be represented as protein as well. Sequence:.*", "Substance may be represented as protein as well"),
                ValidationMessageSubstitution.of("Substance has no UUID, will generate uuid:.*", "Generated UUID because none was supplied"),
                ValidationMessageSubstitution.of("Valence Error on .*", "Valence error on one or more atoms")
        );
        log.trace("initialized substitution list with {}, items", substitutions.size());
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        log.trace("In createIndexableValues");
        if(stagingAreaService ==null) {
            try {
                String contextName = EntityContextLookup.getContextFromEntityClass( importMetadata.getEntityClassName());
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
                        cleanValidationMessage(((ValidationMessage) vm).getMessage())));
            });
            return;
        }
        validations.forEach(v->{
            consumer.accept (IndexableValue.simpleFacetStringValue(IMPORT_METADATA_VALIDATION_TYPE_FACET,
                    String.valueOf((v.getValidationType()))));

            consumer.accept (IndexableValue.simpleFacetStringValue(IMPORT_METADATA_VALIDATION_MESSAGE_FACET,
                    cleanValidationMessage((v.getValidationMessage()))));
        });
    }

    public String cleanValidationMessage(String inputMessage){
        for( ValidationMessageSubstitution substitution : substitutions){
            if(substitution.getToMatch().matcher(inputMessage).find()) {
                return substitution.getSubstitution();
            }
        }
        log.trace("cleanValidationMessage found no match for {}", inputMessage);
        return inputMessage;
    }
}
