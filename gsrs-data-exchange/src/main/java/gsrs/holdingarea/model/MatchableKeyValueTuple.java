package gsrs.holdingarea.model;

import ix.core.models.Indexable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
For Query
Not for persistence
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchableKeyValueTuple {

    public final static String CODE_MATCHABLE_KEY_START =  "CODE_";
    public final static String PRIMARY_NAME_MATCHABLE_KEY="PRIMARY_NAME";
    public final static String SECONDARY_NAME_MATCHABLE_KEY="SECONDARY_NAME";
    public final static String APPROVAL_ID_MATCHABLE_KEY="APPROVAL_ID";
    public final static String UUID_KEY= "UUID";
    public final static String DEFINITIONAL_HASH_KEY ="DEFINITIONAL_HASH";
    public final static int PRIMARY_CODE_LAYER =1;
    public final static int PRIMARY_NAME_LAYER=1;
    public final static int OTHER_NAME_LAYER=2;
    public final static int APPROVAL_ID_LAYER=1;
    public final static int SECONDARY_CODE_LAYER =2;
    public final static int UUID_LAYER=1;
    public final static int DEFINITIONAL_HASH_LAYER=1;

    @Indexable
    private String key;

    @Indexable
    private String value;

    @Indexable
    private String qualifier;

    @Indexable
    private int layer;

    public static MatchableKeyValueTuple of(KeyValueMapping mapping) {
        return MatchableKeyValueTuple.builder()
                .key(mapping.getKey())
                .value(mapping.getValue())
                .qualifier(mapping.getQualifier())
                .build();
    }

    public static MatchableKeyValueTuple of(String key, String value) {
        return MatchableKeyValueTuple.builder()
                .key(key)
                .value(value)
                .build();
    }

    @Override
    public String toString() {
        if (qualifier != null && qualifier.length() > 0) {
            return String.format("MatchableKeyValueTuple: %s = %s/%s", this.getKey(), getValue(), getQualifier());
        } else {
            return String.format("MatchableKeyValueTuple: %s = %s", this.getKey(), getValue());
        }
    }
}
