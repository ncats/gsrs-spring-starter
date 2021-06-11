package gsrs.events;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class BeginReindexEvent {

    private UUID id;
    private long numberOfExpectedRecord;
}
