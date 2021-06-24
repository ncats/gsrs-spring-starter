package gsrs;

import gov.nih.ncats.common.sneak.Sneak;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.events.BackupEvent;
import gsrs.repository.BackupRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.BackupEntity;
import ix.core.models.BaseModel;
import ix.core.models.FetchableEntity;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;

public class BackupEntityProcessorListener {


    private CachedSupplier initializer = CachedSupplier.ofInitializer(()-> AutowireHelper.getInstance().autowire(this));

    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @PostPersist
    @PostUpdate
    public void postPersist(Object o){
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(o);
        if(o instanceof FetchableEntity && ew.getEntityInfo().hasBackup()){
            initializer.get();
            try {
            BackupEntity be = new BackupEntity();
            be.setInstantiated((FetchableEntity) o);
                applicationEventPublisher.publishEvent(new BackupEvent(be));
            } catch (Exception e) {
                Sneak.sneakyThrow(e);
            }


        }
    }

//    @PostUpdate
//    public void postUpdate(Object o){
//        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(o);
//        if(o instanceof BaseModel && ew.getEntityInfo().hasBackup()){
//            initializer.get();
//            BaseModel bm = (BaseModel)o;
//            //this check is done if somehow it's an update but the old version isn't in the backup table
//            BackupEntity be = backupRepository.findByRefid(bm.fetchGlobalId()).orElseGet(()-> new BackupEntity());
//
//            try {
//                be.setInstantiated((BaseModel) o);
//                backupRepository.saveAndFlush(be);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//
//        }
//    }
}
