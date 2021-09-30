package gsrs.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class BeginReindexEvent {

    private UUID id;
    private long numberOfExpectedRecord;


    private WipeIndexStrategy wipeIndexStrategy = WipeIndexStrategy.CLEAR_ALL;

    private List<String> typesToClearFromIndex;

    public enum WipeIndexStrategy{
        CLEAR_ALL,
        NO_OP,
        REMOVE_ONLY_SPECIFIED_TYPES;
    }
}
