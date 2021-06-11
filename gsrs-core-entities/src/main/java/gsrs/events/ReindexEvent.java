package gsrs.events;

public interface ReindexEvent {
    java.util.UUID getReindexId();

    ix.core.util.EntityUtils.Key getEntityKey();
}
