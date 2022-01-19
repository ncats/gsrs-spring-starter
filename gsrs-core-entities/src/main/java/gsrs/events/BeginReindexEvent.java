package gsrs.events;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class BeginReindexEvent implements ReindexOperationEvent {

    private UUID id;
    private long numberOfExpectedRecord;
    @Override
    public UUID getReindexId() {
        return id;
    }
}
