package gsrs;

import ix.core.util.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EntityUpdateEvent {
    private EntityUtils.Key key;
}
