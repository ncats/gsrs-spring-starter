package gsrs.events;

public interface ReindexEvent extends ReindexOperationEvent{
    java.util.UUID getReindexId();
    ix.core.util.EntityUtils.Key getEntityKey();
}
