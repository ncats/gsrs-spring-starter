package gsrs.controller.hateoas;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GsrsControllerInfo {
    private String description;
    private String name;
    private String kind;
    private String href;
    private List<String> supportedOperations;
}
