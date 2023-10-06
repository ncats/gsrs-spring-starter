package gsrs.stagingarea.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SelectableObject {
    String id;
    String name;
    String status;
    String entityClass;
}
