package gsrs.events;

import ix.core.util.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReindexEntityEvent {

    private EntityUtils.Key entityKey;
}
