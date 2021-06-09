package gsrs.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GsrsUrlLink {
    private Class entityClass;
    private String id;
    private String fieldPath;

}
