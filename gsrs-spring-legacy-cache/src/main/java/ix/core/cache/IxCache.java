package ix.core.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PreDestroy;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.cache.GsrsCache;
import gsrs.cache.GsrsLegacyCachePropertyConfiguration;
import ix.core.util.EntityUtils.Key;
import ix.utils.CallableUtil.TypedCallable;
import net.sf.ehcache.Element;

public class IxCache implements GsrsCache {
	private AtomicLong lastNotifiedChange=new AtomicLong(0l); // The last timestamp IxCache was told there was a change
	
    static final int DEFAULT_MAX_ELEMENTS = 10000;
    static final int DEFAULT_TIME_TO_LIVE = 60*60; // 1hr
    static final int DEFAULT_TIME_TO_IDLE = 60*60; // 1hr

    public static final String CACHE_MAX_ELEMENTS = "ix.cache.maxElements";
    public static final String CACHE_MAX_NOT_EVICTABLE_ELEMENTS = "ix.cache.maxElementsNotEvictable";
    public static final String CACHE_TIME_TO_LIVE = "ix.cache.timeToLive";
    public static final String CACHE_TIME_TO_IDLE = "ix.cache.timeToIdle";

    public static final String CACHE_USE_FILEDB = "ix.cache.useFileDb";

    private GateKeeper gateKeeper;

    private GsrsLegacyCachePropertyConfiguration configuration;

    public IxCache(GateKeeper gateKeeper, GsrsLegacyCachePropertyConfiguration configuration) {
        this.gateKeeper =gateKeeper;
        this.configuration= configuration;
    }

    @Override
    public Object getConfiguration() {
        return configuration;
    }

    @PreDestroy
    @Override
    public void close () {
            gateKeeper.close();
        
    }


    @Deprecated
    private Element getElm (String key) {
        return this.gateKeeper.getRawElement(key);
    }


    @Override
    public Object get(String key) {
        return this.gateKeeper.get(key);
    }
    
    
    @Override
    public Object getRaw(String key) {
        return this.gateKeeper.getRaw(key);
    }



    /**
     * apply generator if the evictableCache was created before epoch
     */
    @Override
    public <T> T getOrElse(long epoch,
                           String key, TypedCallable<T> generator) throws Exception {
        return this.gateKeeper.getSinceOrElse(key, epoch, generator);
    }
    
    @Override
    public <T> T getOrElse(String key, TypedCallable<T> generator)
        throws Exception {
    	return this.gateKeeper.getOrElse(key,generator);
    }
    
    
    @Override
    public void clearCache(){
        this.gateKeeper.clear();
    }
    
    @Override
    public boolean remove(String key) {
        return this.gateKeeper.remove(key);
    }
    
    @Override
    public boolean removeAllChildKeys(String key){
        return this.gateKeeper.removeAllChildKeys(key);
    }
    
    public List<CacheStatistics> getStatistics () {
        
        return this.gateKeeper.getStatistics();
    }

    @Override
    public boolean contains(String key) {
        return this.gateKeeper.contains(key);
    }
    


	@Override
    @SuppressWarnings("unchecked")
	public <T> T getOrElseRawIfDirty(String key, TypedCallable<T> generator) throws Exception{
        return this.gateKeeper.getSinceOrElseRaw(key, this.lastNotifiedChange.get(), generator);
	}
	

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrElseIfDirty(String key, TypedCallable<T> generator) throws Exception{
        return this.gateKeeper.getSinceOrElse(key, this.lastNotifiedChange.get(), generator);
    }
	
	
    @Override
    public void setRaw(String key, Object value) {
        this.gateKeeper.putRaw(key, value);
    }

	
	
	@Override
    public void addToMatchingContext(String contextID, Key key, String prop, Object value){
        Map<String,Object> additionalProps = getMatchingContextByContextID(contextID, key.toRootKey());
        if(additionalProps==null){
            additionalProps=new HashMap<String,Object>();
        }
        additionalProps.put(prop, value);
        setMatchingContext(contextID,key.toRootKey(), additionalProps);
    }
	
	@Override
    public void setMatchingContext(String contextID, Key key, Map<String, Object> matchingContext){
	    setRaw("MatchingContext/" + contextID + "/" + key.toString(), matchingContext);
	}
	
	@Override
    public  Map<String, Object> getMatchingContextByContextID(String contextID, Key key){
        return (Map<String, Object>) getRaw("MatchingContext/" + contextID + "/" + key.toString());
    }


	/**
	 * Whenever there's a change to some underlying data store (Lucene, Database, etc)
	 * that this cache will be caching, you can mark it here. Depending on certain
	 * policy considerations, you may be able to reject things older than this time of change
	 * 
	 */
	@Override
	public void markChange() {
		//System.err.println(Util.getExecutionPath());
		this.notifyChange(TimeUtil.getCurrentTimeMillis());
	}
	
	
	/**
	 * Stores the latest of the two time stamps as the time that things 
	 * should be fresh AFTER (possibly stale before this time).
	 * @param time
	 */
	public void notifyChange(long time){
		lastNotifiedChange.updateAndGet(u-> Math.max(u,time));
	}
	
	@Override
	public boolean hasBeenMarkedSince(long thistime){
		if(lastNotifiedChange.get()>thistime)return true;
		return false;
	}
	

	
}
