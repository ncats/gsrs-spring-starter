package gsrs;

import java.util.HashSet;
import java.util.LinkedHashSet;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.events.CreateEditEvent;
import gsrs.repository.EditRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityProcessor;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GsrsEntityProcessorListener {

    @Autowired
    private EntityProcessorFactory epf;
    @Autowired
    private EntityPersistAdapter entityPersistAdapter;

    @Autowired
    private EditRepository editRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;


    private CachedSupplier initializer = CachedSupplier.ofInitializer(()->AutowireHelper.getInstance().autowire(this));
   
    private HashSet<Key> working =new LinkedHashSet<>();
    
    // TP 08-27-2021: In general this shouldn't be necessary,
    // but if a new transactional operation inside a preUpdate
    // hook gets called, this sometimes tries to re-run all preUpdate
    // hooks at the "close" of that transaction. Setting proper isolation
    // levels should help prevent this, but this isn't always obvious.
    // At the time of this comment it's not necessary in any known code
    // to have this on. But it may turn out to be necessary.
    private static boolean PREVENT_RECURSION=false;
    
    
    @Transactional
    @PreUpdate
    public void preUpdate(Object o){
        Key k=null;
        if(PREVENT_RECURSION) {
            k=EntityWrapper.of(o).getKey();
            if(working.contains(k)) {
                log.warn("PostUpdate called, but already updating record for:" + k);
                return;
            }
        }
        
        try {
            if(PREVENT_RECURSION) {
                working.add(k);
            }
            initializer.get();
            entityPersistAdapter.preUpdateBeanDirect(o, ()->epf.getCombinedEntityProcessorFor(o).preUpdate(o));

        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
        } finally {
            if(PREVENT_RECURSION) {
                working.remove(k);
            }
        }
    }

    @PostUpdate
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void postUpdate(Object o){
        try {
            initializer.get();
            //TODO where should oldvalues come from?
            entityPersistAdapter.postUpdateBeanDirect(o, null, true, ()->epf.getCombinedEntityProcessorFor(o).postUpdate(o));

        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
        }
    }

    @PrePersist
    @Transactional
    public void prePersist(Object o){
        try {
            initializer.get();
            epf.getCombinedEntityProcessorFor(o).prePersist(o);
        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
        }
    }

    @PostPersist
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void postPersist(Object o) {
        try {
            initializer.get();
            epf.getCombinedEntityProcessorFor(o).postPersist(o);

        } catch (Throwable e) {
            //I don't think this can happen CombinedEntityProcessor should
            //catch all throwables
            log.error("error calling entityProcessor", e);

        }
        //create an edit?
        EntityUtils.EntityWrapper<?> ew = EntityUtils.EntityWrapper.of(o);
        if(ew.isEntity() && ew.storeHistory() && ew.hasKey()){
            applicationEventPublisher.publishEvent(CreateEditEvent.builder()
                    .kind(o.getClass())
                    .id(ew.getEntityInfo().getNativeIdFor(o).get())
                    .build());

        }
    }

    @PreRemove
    @Transactional
    public void preRemove(Object o){
        try {
            initializer.get();
            epf.getCombinedEntityProcessorFor(o).preRemove(o);
        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
        }
    }
    @PostRemove
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void postRemove(Object o){
        try {
            initializer.get();
            epf.getCombinedEntityProcessorFor(o).postRemove(o);
        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
        }
    }
    @PostLoad
    @Transactional
    public void postLoad(Object o){
        try {
            initializer.get();
//            if(o instanceof AbstractGsrsEntity){
//                ((AbstractGsrsEntity)o).setPreviousState();
//            }
            epf.getCombinedEntityProcessorFor(o).postLoad(o);
        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
        }
    }
}
