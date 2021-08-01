package gsrs;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.events.CreateEditEvent;
import gsrs.model.AbstractGsrsEntity;
import gsrs.repository.EditRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityProcessor;
import ix.core.models.Edit;
import ix.core.util.EntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.*;

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
    @Transactional
    @PreUpdate
    public void preUpdate(Object o){
        try {
            initializer.get();
            entityPersistAdapter.preUpdateBeanDirect(o, ()->epf.getCombinedEntityProcessorFor(o).preUpdate(o));

        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
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
    public void postPersist(Object o){
        try {
            initializer.get();
            epf.getCombinedEntityProcessorFor(o).postPersist(o);
            //create and edit?
           EntityUtils.EntityWrapper<?> ew = EntityUtils.EntityWrapper.of(o);
           if(ew.isEntity() && ew.storeHistory() && ew.hasKey()){
               applicationEventPublisher.publishEvent(CreateEditEvent.builder()
                       .kind(o.getClass())
                       .id(ew.getEntityInfo().getNativeIdFor(o).get())
                        .build());

           }
        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
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
