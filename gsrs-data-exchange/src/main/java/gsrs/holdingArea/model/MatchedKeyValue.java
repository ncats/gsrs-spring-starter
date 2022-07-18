package gsrs.holdingArea.model;

import ix.core.util.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MatchedKeyValue {

    private gsrs.holdingArea.model.MatchableKeyValueTuple tupleUsedInMatching;
    private List<MatchingRecordReference> matchingRecords;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MatchingRecordReference{
        private EntityUtils.Key recordId;
        private String sourceName;

        @Override
        public String toString() {
            return String.format("ID: %s from source %s", recordId, sourceName);
        }
    }
}

