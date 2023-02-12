package gsrs.indexer;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import gsrs.springUtils.AutowireHelper;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.Key;

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

    @Autowired
    private IndexerEventFactoryFactory indexerEventFactoryFactory;

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
            IndexerEventFactory indexerFactoryFor = indexerEventFactoryFactory.getIndexerFactoryFor(obj);
            if(indexerFactoryFor !=null) {
                applicationEventPublisher.publishEvent(indexerFactoryFor.newCreateEventFor(ew));
            }
        }
    }

    @PostUpdate
    public void updateEntity(Object obj) {
        autowireIfNeeded();
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(obj);
        if(ew.shouldIndex()) {
            IndexerEventFactory indexerFactoryFor = indexerEventFactoryFactory.getIndexerFactoryFor(obj);
            if(indexerFactoryFor !=null) {
                applicationEventPublisher.publishEvent(indexerFactoryFor.newUpdateEventFor(ew));
            }
        }
    }
    
    public void reindexEntity(Object obj, boolean deleteFirst) {
        autowireIfNeeded();
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(obj);
        if(ew.shouldIndex()) {
            IndexerEventFactory indexerFactoryFor = indexerEventFactoryFactory.getIndexerFactoryFor(obj);
            if(indexerFactoryFor !=null) {
                applicationEventPublisher.publishEvent(indexerFactoryFor.newReindexEventFor(ew,deleteFirst));
                
            }
        }
    }
    
    @PostRemove
    public void deleteEntity(Object obj){
        autowireIfNeeded();
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(obj);
        if(ew.shouldIndex()) {
            IndexerEventFactory indexerFactoryFor = indexerEventFactoryFactory.getIndexerFactoryFor(obj);
            if(indexerFactoryFor !=null) {
                applicationEventPublisher.publishEvent(indexerFactoryFor.newRemoveEventFor(ew));
            }
        }
    }
}
