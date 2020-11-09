package gsrs.legacy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LegacySearchOptions {

    private String query;
    private Integer top;
    private Integer skip;

}
