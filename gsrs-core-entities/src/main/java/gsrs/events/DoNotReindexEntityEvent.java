package gsrs.events;

import ix.core.util.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class DoNotReindexEntityEvent implements ReindexEvent{

    private UUID reindexId;
    private EntityUtils.Key entityKey;
}
