package gsrs.indexer;

import gsrs.springUtils.AutowireHelper;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import java.io.IOException;

public class IndexerEntityListener {
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private void autowireIfNeeded(){
        if(applicationEventPublisher ==null) {
            AutowireHelper.getInstance().autowire(this);
        }
    }
    @PostPersist
    public void indexNewEntity(Object obj) throws IOException {
//        System.out.println("adding to index " + obj);
        autowireIfNeeded();
        EntityUtils.EntityWrapper<Object> ew = EntityUtils.EntityWrapper.of(obj);
        if(ew.shouldIndex()) {
            applicationEventPublisher.publishEvent(new IndexCreateEntityEvent(ew));
        }
    }

    @PostUpdate
    public void updateEntity(Object obj) throws Exception {
//        System.out.println("updating index " + obj);
        autowireIfNeeded();
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(obj);
        if(ew.shouldIndex()) {
            applicationEventPublisher.publishEvent(new IndexUpdateEntityEvent(EntityUtils.EntityWrapper.of(obj)));
        }
    }
    @PostRemove
    public void deleteEntity(Object obj) throws Exception {
//        System.out.println("removing from index " + obj);
        autowireIfNeeded();
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(obj);
        if(ew.shouldIndex()) {
            applicationEventPublisher.publishEvent(new IndexRemoveEntityEvent(EntityUtils.EntityWrapper.of(obj)));
        }
    }
}
