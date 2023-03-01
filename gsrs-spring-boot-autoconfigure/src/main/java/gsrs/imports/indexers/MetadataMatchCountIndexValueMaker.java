package gsrs.imports.indexers;

import gsrs.holdingarea.model.ImportMetadata;
import gsrs.holdingarea.model.MatchedRecordSummary;
import gsrs.holdingarea.repository.ImportDataRepository;
import gsrs.holdingarea.service.HoldingAreaService;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;

@Slf4j
public class MetadataMatchCountIndexValueMaker implements IndexValueMaker<ImportMetadata> {

    public final static String IMPORT_METADATA_MATCH_COUNT_FACET="Match Count";

    public final static String IMPORT_METADATA_MATCH_KEY_FACET="Match Key";
    @Autowired
    ImportDataRepository importDataRepository;

    @Autowired
    HoldingAreaService holdingAreaService;

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return null;
    }

    private final static String USED_SOURCE= "GSRS";

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        log.trace("In createIndexableValues");
        String instanceData= importDataRepository.retrieveByInstanceID(importMetadata.getInstanceId());
        MatchedRecordSummary matchedRecordSummary = holdingAreaService.findMatchesForJson(importMetadata.getEntityClassName(), instanceData);
        long matchCount= matchedRecordSummary.getMatches().stream()
                        .filter(m->m.getMatchingRecords().stream().anyMatch(r->r.getSourceName().equals(USED_SOURCE)))
                                .count();
        consumer.accept(IndexableValue.simpleLongValue(IMPORT_METADATA_MATCH_COUNT_FACET, matchCount));
        matchedRecordSummary.getMatches().stream()
                        .filter(m->m.getMatchingRecords().stream().anyMatch(r->r.getSourceName().equals(USED_SOURCE)))
                .forEach(r->r.getMatchingRecords()
                .forEach(k-> {
                    consumer.accept(IndexableValue.simpleFacetStringValue(IMPORT_METADATA_MATCH_KEY_FACET, k.getMatchedKey()));
                    log.trace("created facet for key {}",k.getMatchedKey());
                }));

    }
}
