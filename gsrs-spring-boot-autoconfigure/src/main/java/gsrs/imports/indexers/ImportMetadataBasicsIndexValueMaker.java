package gsrs.imports.indexers;

import gsrs.security.GsrsSecurityUtils;
import gsrs.stagingarea.model.ImportMetadata;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/*
Who created this (based on logged-on user)
 */
@Slf4j
public class ImportMetadataBasicsIndexValueMaker implements IndexValueMaker<ImportMetadata> {

    public static final String IMPORT_METADATA_LOADED_BY_FACET="Loaded By";
    public static final String IMPORT_METADATA_IMPORT_DATE_FACET="Loaded By";

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return ImportMetadata.class;
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        consumer.accept(IndexableValue.simpleFacetStringValue(IMPORT_METADATA_LOADED_BY_FACET,
                GsrsSecurityUtils.getCurrentUsername().isPresent() ? GsrsSecurityUtils.getCurrentUsername().get() : "[unknown]"));

    }
}
