package gsrs.indexer;

import ix.core.util.EntityUtils.Key;

/**
 * A Factory to get the {@link IndexerEventFactory} for particular Objects.
 */
public interface IndexerEventFactoryFactory {
    /**
     * Get a {@link IndexerEventFactory} for the given Object to be indexed.
     * @param o the object o index; will never be null.
     * @return an IndexerEventFactory; if the returned object is null, then the object should not be indexed.
     */
    IndexerEventFactory getIndexerFactoryFor(Object o);
    
    /**
     * Get a {@link IndexerEventFactory} for the given Key to be indexed.
     * @param k the Key k index; will never be null.
     * @return an IndexerEventFactory; if the returned object is null, then the object should not be indexed.
     */
    default IndexerEventFactory getIndexerFactoryForKey(Key k) {
    	try {
    		//hack to force backwards compatibility
    		Object o =k.getEntityInfo().getInstance();
    		return getIndexerFactoryFor(o);
    	}catch(Exception e) {
    		
    	}
    	return null;
    }
}
