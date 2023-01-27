package gsrs.imports.indexers;

import gsrs.holdingarea.model.ImportMetadata;
import gsrs.holdingarea.model.MatchedRecordSummary;
import gsrs.holdingarea.repository.ImportDataRepository;
import gsrs.holdingarea.service.HoldingAreaService;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;

public class MetadataMatchCountIndexValueMaker implements IndexValueMaker<ImportMetadata> {

    public final static String IMPORT_METADATA_MATCH_COUNT_FACET="root_importmetadata_matchcount";
    public final static String IMPORT_METADATA_MATCH_KEY_FACET="root_importmetadata_matchkey";
    @Autowired
    ImportDataRepository importDataRepository;

    @Autowired
    HoldingAreaService holdingAreaService;

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return null;
    }

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        String instanceData= importDataRepository.retrieveByInstanceID(importMetadata.getInstanceId());
        MatchedRecordSummary matchedRecordSummary = holdingAreaService.findMatchesForJson(importMetadata.getEntityClassName(), instanceData);
        consumer.accept(IndexableValue.simpleLongValue(IMPORT_METADATA_MATCH_COUNT_FACET, matchedRecordSummary.getMatches().size()));
        matchedRecordSummary.getUniqueMatchingKeys().forEach(k-> consumer.accept(IndexableValue.simpleFacetStringValue(IMPORT_METADATA_MATCH_KEY_FACET, k)));
    }
}
