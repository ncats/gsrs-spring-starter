package ix.core.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.cache.GsrsCache;
import ix.core.util.EntityUtils.Key;
import ix.utils.CallableUtil.TypedCallable;
import net.sf.ehcache.Element;
import net.sf.ehcache.statistics.CoreStatistics;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

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



    public IxCache(GateKeeper gateKeeper) {
        this.gateKeeper =gateKeeper;
    }



    @PreDestroy
    @Override
    public void close () {
            gateKeeper.close();
        
    }



    public Element getElm (String key) {
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
        return this.gateKeeper.getSinceOrElse(key,epoch, generator);
    }
    
    @Override
    public <T> T getOrElse(String key, TypedCallable<T> generator)
        throws Exception {
    	return getOrElse(key,generator,0);
    }
    
    // mimic play.Cache 
    @Override
    public <T> T getOrElse(String key, TypedCallable<T> generator,
                           int seconds) throws Exception {
        return this.gateKeeper.getOrElse(key,  generator,seconds);

    }
    
    @Override
    public void clearCache(){
        this.gateKeeper.clear();
    }
    
    @Override
    public <T> T getOrElseRaw(String key, TypedCallable<T> generator,
                              int seconds) throws Exception {

        return this.gateKeeper.getOrElseRaw(key, generator, seconds);

	}


    public JsonNode toJson(String key){
        Element e = this.gateKeeper.getRawElement(key);
        return new ObjectMapper().valueToTree(e);
    }

    public Stream<Element> toJsonStream(int top, int skip){
        return this.gateKeeper.elements(top,skip);
    }


    
    public void set (String key, Object value) {

        this.gateKeeper.put(key, value);

    }

    public void set (String key, Object value, int expiration) {
        this.gateKeeper.put(key, value, expiration);
    }

    @Override
    public boolean remove(String key) {
        return this.gateKeeper.remove(key);

    }
    
    @Override
    public boolean removeAllChildKeys(String key){
        return this.gateKeeper.removeAllChildKeys(key);

    }
    
   
    
    public List<CoreStatistics> getStatistics () {
        //TODO how to handle multiple caches
        return this.gateKeeper.getStatistics();
    }

    @Override
    public boolean contains(String key) {
        return this.gateKeeper.contains(key);

    }
    


	@Override
    public void setRaw(String key, Object value) {
        this.gateKeeper.putRaw(key, value);

	}


	@Override
    @SuppressWarnings("unchecked")
	public <T> T getOrElseTemp(String key, TypedCallable<T> generator) throws Exception{
        return this.gateKeeper.getOrElseRaw(key, this.lastNotifiedChange.get(), generator,0);
	}
	
	@Override
    @SuppressWarnings("unchecked")
	public <T> T updateTemp(String key, T t) throws Exception{
        this.gateKeeper.putRaw(key, t);
        return t;
	}

	//TODO only used by GLOBAL_CACHE and we could just directly call getOrElseTemp if we have to to avoid the fetch call
//	public static Object getOrFetchTempRecord(Key k) throws Exception {
//		return getOrElseTemp(k.toString(), ()->{
//            Optional<EntityUtils.EntityWrapper<?>> ret = k.fetch();
//            if(ret.isPresent()){
//                return ret.get().getValue();
//            }
//            return null;
//        });
//	}

	/**
	 * Used for temporary cache storage which may be needed across
	 * users or within both attached and detached sessions 
	 * (background threads)
	 * 
	 * Gets a value set from setTemp
	 *  
	 * @param key
	 * @return
	 */
	@Override
    public Object getTemp(String key) {
		return getRaw(key);
	}
	
	
	
	
	/**
	 * Used for temporary cache storage which may be needed across
	 * users or within both attached and detached sessions 
	 * (background threads)
	 * 
	 * Sets the value in such a way that the same key could fetch 
	 * that value, regardless of who is logged in.
	 * 
	 * @param key
	 * @return
	 */
	@Override
    public void setTemp(String key, Object value) {
		setRaw(key, value);
	}
	
	@Override
    public void addToMatchingContext(String contextID, Key key, String prop, Object value){
        Map<String,Object> additionalProps = getMatchingContextByContextID(contextID, key);
        if(additionalProps==null){
            additionalProps=new HashMap<String,Object>();
        }
        additionalProps.put(prop, value);
        setMatchingContext(contextID,key, additionalProps);
    }
	
	@Override
    public void setMatchingContext(String contextID, Key key, Map<String, Object> matchingContext){
	    setTemp("MatchingContext/" + contextID + "/" + key.toString(), matchingContext);
	}
	
	@Override
    public  Map<String, Object> getMatchingContextByContextID(String contextID, Key key){
        return (Map<String, Object>) getTemp("MatchingContext/" + contextID + "/" + key.toString());
    }


	/**
	 * Whenever there's a change to some underlying data store (Lucene, Database, etc)
	 * that this cache will be caching, you can mark it here. Depending on certain
	 * policy considerations, you may be able to reject things older than this time of change
	 * 
	 */
	public void markChange() {
		//System.err.println(Util.getExecutionPath());
		this.notifyChange(System.currentTimeMillis());
	}
	
	
	/**
	 * Stores the latest of the two time stamps as the time that things 
	 * should be fresh AFTER (possibly stale before this time).
	 * @param time
	 */
	public void notifyChange(long time){
		lastNotifiedChange.updateAndGet(u-> Math.max(u,time));
	}
	
	public boolean hasBeenNotifiedSince(long thistime){
		if(lastNotifiedChange.get()>thistime)return true;
		return false;
	}
	
	public boolean mightBeDirtySince(long t){
		
		return this.hasBeenNotifiedSince(t);
	}

	
}
