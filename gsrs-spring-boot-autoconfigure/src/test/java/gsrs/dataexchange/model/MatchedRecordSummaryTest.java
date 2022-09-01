package gsrs.dataexchange.model;

import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.model.MatchedKeyValue;
import gsrs.holdingarea.model.MatchedRecordSummary;
import ix.core.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchedRecordSummaryTest {

    @Test
    public void testgetMultiplyMatchedKeys() {
        System.out.println("testgetMultiplyMatchedKeys");
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
        List<MatchedKeyValue.MatchingRecordReference> records1 = new ArrayList<>();
        UUID recordId1 = UUID.randomUUID();
        UUID recordId2 = UUID.randomUUID();

        records1.add(MatchedKeyValue.MatchingRecordReference.builder()
                .matchedKey("factor1")
                .recordId(EntityUtils.Key.of(Object.class, recordId1))
                .sourceName("Unit Test")
                .build());
        records1.add(MatchedKeyValue.MatchingRecordReference.builder()
                .matchedKey("factor1")
                .recordId(EntityUtils.Key.of(Object.class, recordId2))
                .sourceName("Unit Test")
                .build());
        MatchedKeyValue match1 = MatchedKeyValue.builder()
                .tupleUsedInMatching(tuple1)
                .matchingRecords(records1)
                .build();

        matches.add(MatchedKeyValue.builder()
                .tupleUsedInMatching(tuple1)
                .matchingRecords(records1)
                .build());

        MatchedRecordSummary summary = MatchedRecordSummary.builder()
                .query(query)
                .matches(matches)
                .build();
        List<String> multiplyMatched=summary.getMultiplyMatchedKeys();
        Assertions.assertEquals(0, multiplyMatched.size());
        //Assertions.assertEquals("factor1", multiplyMatched.get(0));
    }
}
