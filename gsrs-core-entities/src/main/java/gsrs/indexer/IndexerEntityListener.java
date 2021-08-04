package gsrs.indexer;

import gsrs.springUtils.AutowireHelper;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import java.io.IOException;

/**
 * JPA Entity Listener that will fire
 * index events on
 * PostPersist, PostUpdate and PostRemove
 * if the entity being persisted/updated/removed is indexable
 * determined by {@link EntityUtils.EntityWrapper#shouldIndex()}.
 */
public class IndexerEntityListener {
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private void autowireIfNeeded(){
        if(applicationEventPublisher ==null) {
            AutowireHelper.getInstance().autowire(this);
        }
    }
    @PostPersist
    public void indexNewEntity(Object obj){
        autowireIfNeeded();
        EntityUtils.EntityWrapper<Object> ew = EntityUtils.EntityWrapper.of(obj);
        if(ew.shouldIndex()) {
            applicationEventPublisher.publishEvent(new IndexCreateEntityEvent(ew.getKey()));
        }
    }

    @PostUpdate
    public void updateEntity(Object obj) {
        autowireIfNeeded();
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(obj);
        if(ew.shouldIndex()) {
            applicationEventPublisher.publishEvent(new IndexUpdateEntityEvent(ew.getKey()));
        }
    }
    @PostRemove
    public void deleteEntity(Object obj){
        autowireIfNeeded();
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(obj);
        if(ew.shouldIndex()) {
            applicationEventPublisher.publishEvent(new IndexRemoveEntityEvent(ew));
        }
    }
}
