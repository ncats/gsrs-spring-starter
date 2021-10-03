package gsrs;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import gov.nih.ncats.common.sneak.Sneak;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.events.BackupEvent;
import gsrs.events.RemoveBackupEvent;
import gsrs.services.BackupService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.FetchableEntity;
import ix.core.util.EntityUtils;

/**
 * JPA Entity Listener
 * that will create {@link BackupEvent}s
 * if the entity being Persisted or Updated
 * is both a {@link FetchableEntity}
 * and should be backed up determined by
 * {@link EntityUtils.EntityInfo#hasBackup()}.
 */
public class BackupEntityProcessorListener {


    private CachedSupplier initializer = CachedSupplier.ofInitializer(()-> AutowireHelper.getInstance().autowire(this));

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private BackupService backupService;



    @PostPersist
    @PostUpdate
    public void postPersist(Object o){
        initializer.get();
         try {
           backupService.backupIfNeeded(o, be-> {

               applicationEventPublisher.publishEvent(new BackupEvent(be));
           });


        } catch (Exception e) {
            Sneak.sneakyThrow(e);
        }

    }

    @PostRemove
    public void postRemove(Object o){
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(o);
        if(o instanceof FetchableEntity && ew.getEntityInfo().hasBackup()){
            initializer.get();
            applicationEventPublisher.publishEvent(RemoveBackupEvent.createFrom((FetchableEntity) o));
        }
    }

}
