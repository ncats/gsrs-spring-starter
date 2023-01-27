package gsrs.imports.indexers;

import gsrs.holdingarea.model.ImportMetadata;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;

import java.util.function.Consumer;

public class MetadataStatusIndexValueMaker implements IndexValueMaker<ImportMetadata> {

    public final static String IMPORT_METADATA_STATUS_FACET="root_importmetadata_status";

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return ImportMetadata.class;
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        consumer.accept(IndexableValue.simpleFacetStringValue(IMPORT_METADATA_STATUS_FACET, importMetadata.getImportStatus().toString()));
    }
}
