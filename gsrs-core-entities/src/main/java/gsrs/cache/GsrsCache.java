package gsrs.cache;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import ix.core.util.EntityUtils;
import ix.utils.CallableUtil;
import lombok.Builder;
import lombok.Data;

public interface GsrsCache extends Closeable {
    Object get(String key);

    Object getRaw(String key);

    <T> T getOrElse (long epoch,
                     String key, CallableUtil.TypedCallable<T> generator) throws Exception;

    <T> T getOrElse (String key, CallableUtil.TypedCallable<T> generator)
                                       throws Exception;

    void clearCache();

    boolean remove(String key);

    boolean removeAllChildKeys(String key);

    boolean contains(String key);

    void setRaw(String key, Object value);

    @SuppressWarnings("unchecked")
    <T> T getOrElseRawIfDirty(String key, CallableUtil.TypedCallable<T> generator) throws Exception;
    

    @SuppressWarnings("unchecked")
    <T> T getOrElseIfDirty(String key, CallableUtil.TypedCallable<T> generator) throws Exception;
//
//    @SuppressWarnings("unchecked")
//    <T> T updateTemp(String key, T t) throws Exception;
//
//    Object getTemp(String key);
//
//    void setTemp(String key, Object value);

    void addToMatchingContext(String contextID, EntityUtils.Key key, String prop, Object value);

    void setMatchingContext(String contextID, EntityUtils.Key key, Map<String, Object> matchingContext);

    Map<String, Object> getMatchingContextByContextID(String contextID, EntityUtils.Key key);

    Object getConfiguration();
    
    public List<CacheStatistics> getStatistics ();
    
    /**
     * Whenever there's a change to some underlying data store (Lucene, Database, etc)
     * that this cache will be caching, you can mark it here. Depending on certain
     * policy considerations, you may be able to reject things older than this time of change
     * 
     */
    public void markChange();
    
    public boolean hasBeenMarkedSince(long thistime);
    
    @Data
    @Builder
    public static class CacheStatistics{
    	private String cacheName;
    	private long maxCacheElements;
    	private long currentCacheElements;
    	private long timeToLive;
    	private long timeToIdle;
    	
    }
}
