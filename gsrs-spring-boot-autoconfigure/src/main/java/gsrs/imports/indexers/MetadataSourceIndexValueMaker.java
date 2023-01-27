package gsrs.imports.indexers;

import gsrs.holdingarea.model.ImportMetadata;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;

import java.util.function.Consumer;

public class MetadataSourceIndexValueMaker implements IndexValueMaker<ImportMetadata> {

    public final static String IMPORT_METADATA_SOURCE_FACET="root_importmetadata_source";

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return ImportMetadata.class;
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        consumer.accept(IndexableValue.simpleFacetStringValue(IMPORT_METADATA_SOURCE_FACET, importMetadata.getSourceName()));
    }
}
