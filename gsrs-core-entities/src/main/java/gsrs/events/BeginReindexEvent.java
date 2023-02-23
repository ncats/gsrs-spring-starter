package gsrs.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class BeginReindexEvent implements ReindexOperationEvent {

    public enum IndexBehavior {
        DO_NOTHING,
        WIPE_ALL_INDEXES,
        WIPE_SPECIFIC_INDEX
    }

    private UUID id;
    private long numberOfExpectedRecord;
    private IndexBehavior indexBehavior= IndexBehavior.DO_NOTHING;
    private List<Class<?>> classesToRemoveFromIndex;

    @Override
    public UUID getReindexId() {
        return id;
    }
}
