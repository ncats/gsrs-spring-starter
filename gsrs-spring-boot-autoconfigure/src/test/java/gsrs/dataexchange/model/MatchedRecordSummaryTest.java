package gsrs.dataexchange.model;

import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.model.MatchedKeyValue;
import gsrs.stagingarea.model.MatchedRecordSummary;
import ix.core.util.EntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
public class MatchedRecordSummaryTest {

    @Test
    public void testGetMultiplyMatchedKeys() {
        log.error("testGetMultiplyMatchedKeys");
        List<MatchableKeyValueTuple> query = new ArrayList<>();
        MatchableKeyValueTuple tuple1 = MatchableKeyValueTuple.builder()
                .key("factor1")
                .value("value1")
                .build();
        query.add(tuple1);
        query.add(MatchableKeyValueTuple.builder()
                .key("factor2")
                .value("value2")
                .build());
        query.add(MatchableKeyValueTuple.builder()
                .key("factor3")
                .value("value3")
                .build());
        List<MatchedKeyValue> matches = new ArrayList<>();
        List<MatchedKeyValue.MatchingRecordReference> recordsList1 = new ArrayList<>();
        UUID recordId1 = UUID.randomUUID();
        UUID recordId2 = UUID.randomUUID();
        UUID recordId3 = UUID.randomUUID();
        UUID recordId4 = UUID.randomUUID();
        UUID recordId5 = UUID.randomUUID();

        recordsList1.add(MatchedKeyValue.MatchingRecordReference.builder()
                .matchedKey("factor1")
                .recordId(EntityUtils.Key.of(Object.class, recordId1))
                .sourceName("Unit Test")
                .build());
        recordsList1.add(MatchedKeyValue.MatchingRecordReference.builder()
                .matchedKey("factor1")
                .recordId(EntityUtils.Key.of(Object.class, recordId2))
                .sourceName("Unit Test")
                .build());
        MatchedKeyValue match1 = MatchedKeyValue.builder()
                .tupleUsedInMatching(tuple1)
                .matchingRecords(recordsList1)
                .build();

        matches.add(MatchedKeyValue.builder()
                .tupleUsedInMatching(tuple1)
                .matchingRecords(recordsList1)
                .build());

        List<MatchedKeyValue.MatchingRecordReference> recordsList2 = new ArrayList<>();
        recordsList2.add(MatchedKeyValue.MatchingRecordReference.builder()
                .matchedKey("factor1")
                .recordId(EntityUtils.Key.of(Object.class, recordId3))
                .sourceName("Unit Test")
                .build());
        matches.add(MatchedKeyValue.builder()
                .tupleUsedInMatching(tuple1)
                .matchingRecords(recordsList2)
                .build());
        MatchedRecordSummary summary = MatchedRecordSummary.builder()
                .query(query)
                .matches(matches)
                .build();
        List<String> multiplyMatched=summary.getMultiplyMatchedKeys();
        Assertions.assertEquals(1, multiplyMatched.size());
        Assertions.assertEquals("factor1", multiplyMatched.get(0));
    }

    @Test
    public void testGetUniqueMatchedKeys() {
        log.debug("testGetMultiplyMatchedKeys");
        List<MatchableKeyValueTuple> query = new ArrayList<>();
        MatchableKeyValueTuple tuple1 = MatchableKeyValueTuple.builder()
                .key("factor1")
                .value("value1")
                .build();
        query.add(tuple1);
        query.add(MatchableKeyValueTuple.builder()
                .key("factor2")
                .value("value2")
                .build());
        query.add(MatchableKeyValueTuple.builder()
                .key("factor3")
                .value("value3")
                .build());
        List<MatchedKeyValue> matches = new ArrayList<>();
        List<MatchedKeyValue.MatchingRecordReference> recordsList1 = new ArrayList<>();
        UUID recordId1 = UUID.randomUUID();
        UUID recordId2 = UUID.randomUUID();
        UUID recordId3 = UUID.randomUUID();

        recordsList1.add(MatchedKeyValue.MatchingRecordReference.builder()
                .matchedKey("factor1")
                .recordId(EntityUtils.Key.of(Object.class, recordId1))
                .sourceName("Unit Test")
                .build());
        recordsList1.add(MatchedKeyValue.MatchingRecordReference.builder()
                .matchedKey("factor1")
                .recordId(EntityUtils.Key.of(Object.class, recordId2))
                .sourceName("Unit Test")
                .build());

        matches.add(MatchedKeyValue.builder()
                .tupleUsedInMatching(tuple1)
                .matchingRecords(recordsList1)
                .build());

        List<MatchedKeyValue.MatchingRecordReference> recordsList2 = new ArrayList<>();
        recordsList2.add(MatchedKeyValue.MatchingRecordReference.builder()
                .matchedKey("factor1")
                .recordId(EntityUtils.Key.of(Object.class, recordId3))
                .sourceName("Unit Test")
                .build());
        matches.add(MatchedKeyValue.builder()
                .tupleUsedInMatching(tuple1)
                .matchingRecords(recordsList2)
                .build());
        MatchedRecordSummary summary = MatchedRecordSummary.builder()
                .query(query)
                .matches(matches)
                .build();
        List<String> multiplyMatched=summary.getUniqueMatchingKeys();
        Assertions.assertEquals(3, multiplyMatched.size());
        System.out.println("multiply matched:");
        multiplyMatched.forEach(System.out::println);
        List<String> expected = Arrays.asList("factor1", "factor2", "factor3");
        Assertions.assertTrue(multiplyMatched.containsAll(expected));
    }

    @Test
    public void testGetMultiplyMatchedKeys0() {
        log.trace("testGetMultiplyMatchedKeys0 - expecting 0 multiply matched keys");
        List<MatchableKeyValueTuple> query = new ArrayList<>();
        MatchableKeyValueTuple tuple1 = MatchableKeyValueTuple.builder()
                .key("factor1")
                .value("value1")
                .build();
        query.add(tuple1);
        query.add(MatchableKeyValueTuple.builder()
                .key("factor2")
                .value("value2")
                .build());
        query.add(MatchableKeyValueTuple.builder()
                .key("factor3")
                .value("value3")
                .build());
        List<MatchedKeyValue> matches = new ArrayList<>();
        List<MatchedKeyValue.MatchingRecordReference> recordsList1 = new ArrayList<>();
        UUID recordId1 = UUID.randomUUID();
        UUID recordId2 = UUID.randomUUID();
        UUID recordId3 = UUID.randomUUID();

        recordsList1.add(MatchedKeyValue.MatchingRecordReference.builder()
                .matchedKey("factor1")
                .recordId(EntityUtils.Key.of(Object.class, recordId1))
                .sourceName("Unit Test")
                .build());

        matches.add(MatchedKeyValue.builder()
                .tupleUsedInMatching(tuple1)
                .matchingRecords(recordsList1)
                .build());

        List<MatchedKeyValue.MatchingRecordReference> recordsList2 = new ArrayList<>();
        recordsList2.add(MatchedKeyValue.MatchingRecordReference.builder()
                .matchedKey("factor1a")
                .recordId(EntityUtils.Key.of(Object.class, recordId3))
                .sourceName("Unit Test")
                .build());
        MatchableKeyValueTuple tuple2 = MatchableKeyValueTuple.builder()
                .key("factor1a")
                .value("value1a")
                .build();
        matches.add(MatchedKeyValue.builder()
                .tupleUsedInMatching(tuple2)
                .matchingRecords(recordsList2)
                .build());
        MatchedRecordSummary summary = MatchedRecordSummary.builder()
                .query(query)
                .matches(matches)
                .build();
        List<String> multiplyMatched=summary.getMultiplyMatchedKeys();
        Assertions.assertEquals(0, multiplyMatched.size());

    }

}
