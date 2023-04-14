package gsrs.imports.indexers;

import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.imports.GsrsImportAdapterFactoryFactory;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.model.MatchedRecordSummary;
import gsrs.stagingarea.repository.ImportDataRepository;
import gsrs.stagingarea.service.StagingAreaService;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

@Slf4j
public class MetadataMatchCountIndexValueMaker implements IndexValueMaker<ImportMetadata> {

    public final static String IMPORT_METADATA_MATCH_COUNT_FACET="Match Count";

    public final static String IMPORT_METADATA_MATCH_KEY_FACET="Match Key";

    @Autowired
    ImportDataRepository importDataRepository;

    @Autowired
    private GsrsImportAdapterFactoryFactory gsrsImportAdapterFactoryFactory;

    StagingAreaService stagingAreaService;

    @Override
    public Class<ImportMetadata> getIndexedEntityClass() {
        return ImportMetadata.class;
    }

    private final static String USED_SOURCE= "GSRS";

    @Override
    public void createIndexableValues(ImportMetadata importMetadata, Consumer<IndexableValue> consumer) {
        log.trace("In createIndexableValues");
        if(stagingAreaService ==null) {
            try {
                String contextName = importMetadata.getEntityClassName();
                //hack!
                if(contextName.contains(".")) {
                    String[] parts =contextName.split("\\.");
                    contextName = parts[parts.length-1].toLowerCase() + "s";
                }
                stagingAreaService =gsrsImportAdapterFactoryFactory.getStagingAreaService(contextName);
            } catch (NoSuchMethodException |InvocationTargetException  | InstantiationException | IllegalAccessException e) {
                log.error("Error creating staging area service!");
                throw new RuntimeException(e);
            }
        }
        String instanceData= importDataRepository.retrieveByInstanceID(importMetadata.getInstanceId());
        MatchedRecordSummary matchedRecordSummary = stagingAreaService.findMatchesForJson(importMetadata.getEntityClassName(), instanceData,
                importMetadata.getRecordId().toString());
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
