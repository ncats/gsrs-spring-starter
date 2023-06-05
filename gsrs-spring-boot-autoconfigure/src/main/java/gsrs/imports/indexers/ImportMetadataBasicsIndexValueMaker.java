package gsrs.imports.indexers;

import gsrs.stagingarea.model.ImportMetadata;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.function.Consumer;

/*
Who created this (based on logged-on user)
 */
@Slf4j
public class ImportMetadataBasicsIndexValueMaker implements IndexValueMaker<ImportMetadata> {

    public static final String IMPORT_METADATA_IMPORT_DATE_FACET="Date Loaded";

    private final String pattern = "dd-MMM-yyyy";//"dd-MMM-yyyy HH:mm:ss";
    DateFormat dateFormat = new SimpleDateFormat(pattern);

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return ImportMetadata.class;
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        log.trace("starting createIndexableValues");
        String dateLoaded = "";
        if( importMetadata.getVersionCreationDate()!=null){
            dateLoaded=dateFormat.format(importMetadata.getVersionCreationDate());
        }
        log.trace("value: {}", dateLoaded);
        consumer.accept(IndexableValue.simpleFacetStringValue(IMPORT_METADATA_IMPORT_DATE_FACET, dateLoaded));

    }
}
