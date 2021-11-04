package gsrs.events;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class EndReindexEvent implements ReindexOperationEvent{

    private UUID id;

    @Override
    public UUID getReindexId() {
        return id;
    }
}
