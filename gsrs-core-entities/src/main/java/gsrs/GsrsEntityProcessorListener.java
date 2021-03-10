package gsrs;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.persistence.*;

@Slf4j
public class GsrsEntityProcessorListener {

    @Autowired
    private EntityProcessorFactory epf;
    @Autowired
    private EntityPersistAdapter entityPersistAdapter;

    private CachedSupplier initializer = CachedSupplier.ofInitializer(()->AutowireHelper.getInstance().autowire(this));
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
    public void prePersist(Object o){
        try {
            initializer.get();
            epf.getCombinedEntityProcessorFor(o).prePersist(o);
        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
        }
    }

    @PostPersist
    public void postPersist(Object o){
        try {
            initializer.get();
            epf.getCombinedEntityProcessorFor(o).postPersist(o);
        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
        }
    }

    @PreRemove
    public void preRemove(Object o){
        try {
            initializer.get();
            epf.getCombinedEntityProcessorFor(o).preRemove(o);
        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
        }
    }
    @PostRemove
    public void postRemove(Object o){
        try {
            initializer.get();
            epf.getCombinedEntityProcessorFor(o).postRemove(o);
        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
        }
    }
    @PostLoad
    public void postLoad(Object o){
        try {
            initializer.get();
            epf.getCombinedEntityProcessorFor(o).postLoad(o);
        } catch (EntityProcessor.FailProcessingException e) {
            log.error("error calling entityProcessor", e);
        }
    }
}
