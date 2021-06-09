package gsrs.events;

import ix.core.models.BackupEntity;
import org.springframework.context.ApplicationEvent;

public class BackupEvent extends ApplicationEvent {

    public BackupEvent(BackupEntity backupEntity) {
        super(backupEntity);
    }

    @Override
    public BackupEntity getSource() {
        return (BackupEntity) super.getSource();
    }
}
