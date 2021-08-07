package gsrs.services;

import gsrs.events.BackupEvent;
import gsrs.events.RemoveBackupEvent;
import gsrs.repository.BackupRepository;
import ix.core.models.BackupEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Service
public class BackupService {

    private BackupRepository backupRepository;
    @Autowired
    public BackupService(BackupRepository backupRepository){
        this.backupRepository = backupRepository;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void backup(BackupEvent event) throws Exception {
        BackupEntity be = event.getSource();
        Optional<BackupEntity> old = backupRepository.findByRefid(be.getRefid());
        if(old.isPresent()){
            BackupEntity updated= old.get();
            updated.setFromOther(be);
            backupRepository.saveAndFlush(updated);
        }else{
            backupRepository.saveAndFlush(be);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteBackup(RemoveBackupEvent event) throws Exception {
        Optional<BackupEntity> old = backupRepository.findByRefid(event.getRefid());
        if(old.isPresent()){
            backupRepository.delete(old.get());
        }
    }
}
