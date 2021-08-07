package gsrs.events;

import ix.core.models.FetchableEntity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RemoveBackupEvent {
    private String refid;

    /**
     * Create a new RemoveBackupEvent from a {@link FetchableEntity} object.
     * This factory method knows how to generate the refid from the passed in
     * entity.
     * @param e
     * @return
     */
    public static RemoveBackupEvent createFrom(FetchableEntity e){
        return RemoveBackupEvent.builder()
                .refid(e.fetchGlobalId())
                .build();
    }
}
