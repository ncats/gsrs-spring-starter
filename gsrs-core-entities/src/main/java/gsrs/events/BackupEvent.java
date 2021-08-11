package gsrs.events;

import ix.core.models.BackupEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackupEvent {

    private BackupEntity source;

    private UUID reBackupTaskId;

    public BackupEvent(BackupEntity source){
        this.source = source;
    }

}
