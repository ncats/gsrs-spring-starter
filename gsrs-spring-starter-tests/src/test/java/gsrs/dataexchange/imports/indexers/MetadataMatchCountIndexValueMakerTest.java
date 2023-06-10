package gsrs.dataexchange.imports.indexers;

import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.model.MatchedKeyValue;
import gsrs.stagingarea.model.MatchedRecordSummary;
import gsrs.stagingarea.repository.ImportDataRepository;
import gsrs.stagingarea.service.StagingAreaService;
import gsrs.imports.indexers.MetadataMatchCountIndexValueMaker;
import ix.core.models.Group;
import ix.core.search.text.IndexableValue;
import ix.core.util.EntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
public class MetadataMatchCountIndexValueMakerTest {

    @Test
    public void createIndexableValuesTest() throws NoSuchFieldException, IllegalAccessException {
        ImportMetadata metadata = new ImportMetadata();
        metadata.setAccess( Collections.singleton(new Group("protected")));
        metadata.setReason("test");
        String sourceName = "Unique Data Source";
        metadata.setSourceName(sourceName);
        metadata.setEntityClassName("ix.ginas.models.v1.Substance");
        UUID record1IdValue = UUID.randomUUID();
        String record1Id = record1IdValue.toString();
        metadata.setRecordId(record1IdValue);
        StagingAreaService stagingAreaService = mock(StagingAreaService.class);
        ImportDataRepository importDataRepository = mock(ImportDataRepository.class);
        MatchedRecordSummary matchedRecordSummary = new MatchedRecordSummary();
        List<MatchedKeyValue> matchedKeyValues = new ArrayList<>();
        List<MatchedKeyValue.MatchingRecordReference> matchingRecords = new ArrayList<>();
        MatchedKeyValue.MatchingRecordReference match1= new MatchedKeyValue.MatchingRecordReference();
        match1.setMatchedKey("Factor 1");
        match1.setSourceName("GSRS");
        EntityUtils.Key recordId=EntityUtils.Key.of(ImportMetadata.class, record1Id);
        match1.setRecordId( recordId);
        matchingRecords.add(match1);
        MatchedKeyValue mkv1= new MatchedKeyValue();
        mkv1.setMatchingRecords(matchingRecords);
        MatchableKeyValueTuple tuple1 =MatchableKeyValueTuple.builder()
                .key("Factor 1")
                .value("Value 1")
                .build();
        mkv1.setTupleUsedInMatching(tuple1);
        matchedKeyValues.add(mkv1);
        MatchedKeyValue mkv2= new MatchedKeyValue();
        mkv2.setMatchingRecords(matchingRecords);
        MatchableKeyValueTuple tuple2 =MatchableKeyValueTuple.builder()
                .key("Factor 2")
                .value("Value 2")
                .build();
        mkv2.setTupleUsedInMatching(tuple2);
        matchedKeyValues.add(mkv2);
        matchedRecordSummary.setMatches(matchedKeyValues);
        List<MatchableKeyValueTuple> query =  new ArrayList<>();
        query.add(tuple1);
        query.add(tuple2);
        matchedRecordSummary.setQuery(query);
        String recordJson = "";
        when(stagingAreaService.findMatchesForJson(metadata.getEntityClassName(), recordJson, null)).thenReturn(matchedRecordSummary);
        when(stagingAreaService.findMatchesForJson(metadata.getEntityClassName(), recordJson, record1Id)).thenReturn(matchedRecordSummary);
        when(importDataRepository.retrieveByInstanceID(metadata.getInstanceId())).thenReturn(recordJson);

        List<IndexableValue> indexedValues = new ArrayList<>();
        MetadataMatchCountIndexValueMaker indexValueMaker1 = new MetadataMatchCountIndexValueMaker();
        Field repoField = indexValueMaker1.getClass().getDeclaredField("importDataRepository");
        repoField.setAccessible(true);
        Field serviceField = indexValueMaker1.getClass().getDeclaredField("stagingAreaService");
        serviceField.setAccessible(true);
        repoField.set(indexValueMaker1, importDataRepository);
        serviceField.set(indexValueMaker1, stagingAreaService);;
        indexValueMaker1.createIndexableValues(metadata, indexedValues::add);
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().equals(MetadataMatchCountIndexValueMaker.IMPORT_METADATA_MATCH_COUNT_FACET)
                && (i.value().equals("1"))));
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().equals(MetadataMatchCountIndexValueMaker.IMPORT_METADATA_MATCH_KEY_FACET)
        && i.value().equals("Factor 1")));
    }

    @Test
    public void createIndexableValuesTest2() throws NoSuchFieldException, IllegalAccessException {
        ImportMetadata metadata = new ImportMetadata();
        metadata.setAccess( Collections.singleton(new Group("protected")));
        metadata.setReason("test");
        String sourceName = "Unique Data Source";
        metadata.setSourceName(sourceName);
        metadata.setEntityClassName("ix.ginas.models.v1.Substance");
        UUID record1IdValue = UUID.randomUUID();
        String record1Id = record1IdValue.toString();
        metadata.setRecordId(record1IdValue);
        StagingAreaService stagingAreaService = mock(StagingAreaService.class);
        ImportDataRepository importDataRepository = mock(ImportDataRepository.class);
        MatchedRecordSummary matchedRecordSummary = new MatchedRecordSummary();
        List<MatchedKeyValue> matchedKeyValues = new ArrayList<>();
        List<MatchedKeyValue.MatchingRecordReference> matchingRecords = new ArrayList<>();
        MatchedKeyValue.MatchingRecordReference match1= new MatchedKeyValue.MatchingRecordReference();
        match1.setMatchedKey("Factor 1");
        match1.setSourceName("GSRS");
        EntityUtils.Key recordId=EntityUtils.Key.of(ImportMetadata.class, record1Id);
        match1.setRecordId( recordId);
        matchingRecords.add(match1);

        String record2Id = UUID.randomUUID().toString();
        EntityUtils.Key recordId2=EntityUtils.Key.of(ImportMetadata.class, record2Id);
        MatchedKeyValue.MatchingRecordReference match2= new MatchedKeyValue.MatchingRecordReference();
        match2.setRecordId( recordId2);
        match2.setMatchedKey("Factor 1");
        match2.setSourceName("GSRS");
        matchingRecords.add(match2);

        MatchedKeyValue mkv1= new MatchedKeyValue();
        mkv1.setMatchingRecords(matchingRecords);
        MatchableKeyValueTuple tuple1 =MatchableKeyValueTuple.builder()
                .key("Factor 1")
                .value("Value 1")
                .build();
        mkv1.setTupleUsedInMatching(tuple1);
        matchedKeyValues.add(mkv1);
        MatchedKeyValue mkv2= new MatchedKeyValue();
        mkv2.setMatchingRecords(matchingRecords);
        MatchableKeyValueTuple tuple2 =MatchableKeyValueTuple.builder()
                .key("Factor 2")
                .value("Value 2")
                .build();
        mkv2.setTupleUsedInMatching(tuple2);
        matchedKeyValues.add(mkv2);
        matchedRecordSummary.setMatches(matchedKeyValues);
        List<MatchableKeyValueTuple> query =  new ArrayList<>();
        query.add(tuple1);
        query.add(tuple2);
        matchedRecordSummary.setQuery(query);
        String recordJson = "";
        when(stagingAreaService.findMatchesForJson(metadata.getEntityClassName(), recordJson, null)).thenReturn(matchedRecordSummary);
        when(stagingAreaService.findMatchesForJson(metadata.getEntityClassName(), recordJson, record1Id)).thenReturn(matchedRecordSummary);
        when(stagingAreaService.findMatchesForJson(metadata.getEntityClassName(), recordJson, record2Id)).thenReturn(matchedRecordSummary);
        when(importDataRepository.retrieveByInstanceID(metadata.getInstanceId())).thenReturn(recordJson);

        List<IndexableValue> indexedValues = new ArrayList<>();
        MetadataMatchCountIndexValueMaker indexValueMaker1 = new MetadataMatchCountIndexValueMaker();
        Field repoField = indexValueMaker1.getClass().getDeclaredField("importDataRepository");
        repoField.setAccessible(true);
        Field serviceField = indexValueMaker1.getClass().getDeclaredField("stagingAreaService");
        serviceField.setAccessible(true);
        repoField.set(indexValueMaker1, importDataRepository);
        serviceField.set(indexValueMaker1, stagingAreaService);
        String expectedMatchCount = Integer.toString(2);
        indexValueMaker1.createIndexableValues(metadata, indexedValues::add);
        Assertions.assertTrue(indexedValues.stream().anyMatch(i->i.name().equals(MetadataMatchCountIndexValueMaker.IMPORT_METADATA_MATCH_COUNT_FACET)
                && (i.value().equals(expectedMatchCount))));
    }
}
