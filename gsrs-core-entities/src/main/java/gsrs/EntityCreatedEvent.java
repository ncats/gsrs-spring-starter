package gsrs;

import ix.core.util.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EntityCreatedEvent {
    private EntityUtils.Key key;
}
