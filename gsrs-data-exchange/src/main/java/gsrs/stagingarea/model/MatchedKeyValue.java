package gsrs.stagingarea.model;

import ix.core.util.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
a key/value pair that has located one or more records in a Staging Area or permanent GSRS database
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MatchedKeyValue {

    private gsrs.stagingarea.model.MatchableKeyValueTuple tupleUsedInMatching;
    private List<MatchingRecordReference> matchingRecords;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MatchingRecordReference{

        /**
         * reference to another record
         */
        private EntityUtils.Key recordId;

        /**
         * location of the matching data (GSRS or staging area)
         */
        private String sourceName;

        /**
         * ID of matching record
         */
        private String matchedKey;

        @Override
        public String toString() {
            return String.format("ID: %s from source %s", recordId, sourceName);
        }
    }
}

