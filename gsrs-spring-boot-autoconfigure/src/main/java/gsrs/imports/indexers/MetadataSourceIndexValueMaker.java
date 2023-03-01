package gsrs.imports.indexers;

import gsrs.holdingarea.model.ImportMetadata;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class MetadataSourceIndexValueMaker implements IndexValueMaker<ImportMetadata> {

    public final static String IMPORT_METADATA_SOURCE_FACET="Source";

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return ImportMetadata.class;
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        log.trace("starting in createIndexableValues");
        consumer.accept(IndexableValue.simpleFacetStringValue(IMPORT_METADATA_SOURCE_FACET, importMetadata.getSourceName()));
    }
}
