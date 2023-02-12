package gsrs.indexer;

import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.Key;

import org.springframework.core.Ordered;

import gsrs.events.ReindexEntityEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Factory to create Indexing events for a given Object.
 * GSRS will have several IndexerEventFactories loaded
 * and when an Object is to be indexed, it will iterate through each one
 * of these Factories to see which one to use.
 * The first supported Factory that is found will have its corresponding
 * {@link #newCreateEventFor(EntityUtils.EntityWrapper)},
 * {@link #newUpdateEventFor(EntityUtils.EntityWrapper)}
 * or {@link #newRemoveEventFor(EntityUtils.EntityWrapper)} method
 * with the Object passed in to {@link #supports(Object)} wrapped in an {@link ix.core.util.EntityUtils.EntityWrapper}.
 *
 * By default, the default implementations will make the base {@link IndexCreateEntityEvent}, {@link IndexUpdateEntityEvent}
 * and {@link IndexRemoveEntityEvent} respectively.
 */
public interface IndexerEventFactory extends Ordered {
    /**
     * Create a new CreateIndexEvent object for the given wrapped Entity.
     * The returned Object will be published to the {@link org.springframework.context.ApplicationEventPublisher}.
     * @param ew the {@link ix.core.util.EntityUtils.EntityWrapper} wrapping the Object that returned {@code true}
     *           if it was supported by this indexerFactory.
     * @return the event object to publish; or {@code null} if nothing should be published.
     *
     */
    default Object newCreateEventFor(EntityUtils.EntityWrapper ew){
        Optional<EntityUtils.Key> optKey = ew.getOptionalKey();
        if(optKey.isPresent()) {
            return new IndexCreateEntityEvent(optKey.get(), Optional.of(ew));
        }
        return null;
    }
    /**
     * Create a new ReindexEntityEvent object for the given wrapped Entity.
     * The returned Object will be published to the {@link org.springframework.context.ApplicationEventPublisher}.
     * @param ew the {@link ix.core.util.EntityUtils.EntityWrapper} wrapping the Object that returned {@code true}
     *           if it was supported by this indexerFactory.
     * @param deleteFirst a boolean flag to specify whether this reindexing needs a delete first. if true it will
     *           act similarly to a normal update event.     
     * @return the event object to publish; or {@code null} if nothing should be published.
     */
    default Object newReindexEventFor(EntityUtils.EntityWrapper ew, boolean deleteFirst){
        return new ReindexEntityEvent(UUID.randomUUID(), ew.getKey() ,Optional.of(ew),deleteFirst);
    }
    
    /**
     * Create a new UpdateIndexEvent object for the given wrapped Entity.
     * The returned Object will be published to the {@link org.springframework.context.ApplicationEventPublisher}.
     * @param ew the {@link ix.core.util.EntityUtils.EntityWrapper} wrapping the Object that returned {@code true}
     *           if it was supported by this indexerFactory.
     * @return the event object to publish; or {@code null} if nothing should be published.
     */
    default Object newUpdateEventFor(EntityUtils.EntityWrapper ew){
        Optional<EntityUtils.Key> optKey = ew.getOptionalKey();
        if(optKey.isPresent()) {
            return new IndexUpdateEntityEvent(optKey.get());
        }
        return null;
    }
    
    /**
     * Create a new RemoveIndexEvent object for the given wrapped Entity.
     * The returned Object will be published to the {@link org.springframework.context.ApplicationEventPublisher}.
     * @param ew the {@link ix.core.util.EntityUtils.EntityWrapper} wrapping the Object that returned {@code true}
     *           if it was supported by this indexerFactory.
     * @return the event object to publish; or {@code null} if nothing should be published.
     */
    default Object newRemoveEventFor(EntityUtils.EntityWrapper ew){
        return new IndexRemoveEntityEvent(ew);
    }

    /**
     * Does this Factory support this object for indexing?
     * @param object the object to be indexed; will never be null.
     * @return {@code true} if it can; {@code false} otherwise.
     */
    boolean supports(Object object);


    /**
     * Does this Factory support this key for indexing?
     * @param k the Key of the object to be indexed
     * @return {@code true} if it can; {@code false} otherwise.
     */
    default boolean supportsKey(Key k) {
    	try {
    		//hack to force backwards compatibility
    		Object o =k.getEntityInfo().getInstance();
    		return supports(o);
    	}catch(Exception e) {
    		
    	}
    	return false;
    }
    
    /**
     * The sort order to provide when iterating through the list of all
     * known factories.  The lower the number the earlier in the list it will be.
     * Do not use the Integer.MAX_VALUE OR {@link Ordered#LOWEST_PRECEDENCE}
     * as that is reserved for {@link DefaultIndexerEventFactory} so it is always last.
     * @return an int the lower the earlier this object will appear in Spring sorted autowired lists.
     */
    @Override
    int getOrder();
}
