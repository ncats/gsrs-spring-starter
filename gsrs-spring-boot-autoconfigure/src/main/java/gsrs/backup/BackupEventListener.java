package gsrs.backup;

import gsrs.events.BackupEvent;
import gsrs.events.RemoveBackupEvent;
import gsrs.services.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BackupEventListener {

    @Autowired
    private BackupService backupService;


    @TransactionalEventListener
    public void onBackupEvent(BackupEvent event) throws Exception {
        backupService.backup(event);
    }

    @TransactionalEventListener
    public void onRemoveBackupEvent(RemoveBackupEvent event) throws Exception {
        backupService.deleteBackup(event);
    }
}
