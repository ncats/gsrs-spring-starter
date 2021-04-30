package gsrs.backup;

import gsrs.events.BackupEvent;
import gsrs.repository.BackupRepository;
import ix.core.models.BackupEntity;
import ix.utils.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;


public class BackupEventListener {

    @Autowired
    private BackupRepository backupRepository;

    @PostConstruct
    public void init(){

        System.out.println("here");
    }
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBackupEvent(BackupEvent event) throws Exception {
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
}
