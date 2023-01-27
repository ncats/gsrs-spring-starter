package gsrs.dataexchange.imports.indexers;

import gsrs.imports.indexers.MetadataStatusIndexValueMaker;
import gsrs.holdingarea.model.ImportMetadata;
import ix.core.models.Group;
import ix.core.search.text.IndexableValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MetadataStatusIndexValueMakerTest {

    @Test
    public void createIndexableValuesTest(){
        ImportMetadata metadata = new ImportMetadata();
        metadata.setAccess( Collections.singleton(new Group("protected")));
        metadata.setReason("test");
        String sourceName = "Unique Data Source";
        metadata.setSourceName(sourceName);
        metadata.setImportStatus(ImportMetadata.RecordImportStatus.rejected);
        metadata.setEntityClassName("ix.ginas.models.v1.Substance");

        MetadataStatusIndexValueMaker indexValueMaker = new MetadataStatusIndexValueMaker();
        List<IndexableValue> values = new ArrayList<>();
        indexValueMaker.createIndexableValues(metadata, values::add);
        Assertions.assertEquals(ImportMetadata.RecordImportStatus.rejected.toString(), values.get(0).value());
        Assertions.assertEquals(MetadataStatusIndexValueMaker.IMPORT_METADATA_STATUS_FACET, values.get(0).name());
    }
}
