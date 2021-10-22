package ix.core;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.apache.poi.ss.formula.functions.T;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.cache.GsrsCache;
import gsrs.repository.BackupRepository;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.models.BackupEntity;
import ix.core.search.LazyList.NamedCallable;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.Key;
import ix.utils.CallableUtil.TypedCallable;


/**
 * Utility "wrapper" for producing an entity from some source.
 * 
 * Currently, it accepts a Key, and will generate a value on 
 * call, by various sources, depending on the CacheType.
 * 
 * @author peryeata
 *
 * @param <T>
 */
public class EntityFetcher<T> implements NamedCallable<Key,T>{
    private static Object getOrFetchTempRecord(Key k) throws Exception {
        GsrsCache ixcache = getIxCache();
        return ixcache.getOrElseTemp(k.toString(), ()->{
            Optional<EntityUtils.EntityWrapper<?>> ret = k.fetch();
            if(ret.isPresent()){
                return ret.get().getValue();
            }
            return null;
        });
    }
    
    private static GsrsCache getIxCache() {
        GsrsCache cache = StaticContextAccessor.getBean(GsrsCache.class);
        return cache;
    }
    
    private static BackupRepository getBackupRepository() {
        BackupRepository repo = StaticContextAccessor.getBean(BackupRepository.class);
        return repo;
    }

	public enum CacheType{
		/**
		 * Don't use a cache always refetch from db.
		 */
		NO_CACHE{
			@Override
			<T> T get(EntityFetcher<T> fetcher) throws Exception {
				return (T) fetcher.findObject();
			}
		},
		/**
		 * Everyone sees everything (works)
		 */
		GLOBAL_CACHE{
			@Override
			<T> T get(EntityFetcher<T> fetcher) throws Exception{
				return (T) getOrFetchTempRecord(fetcher.theKey);
			}
		},
		/**
		 * look at last indexing, is it older then last time this was put?
		 */
		GLOBAL_CACHE_WHEN_NOT_CHANGED{
			@Override
			<T> T get(EntityFetcher<T> fetcher) throws Exception {
			    GsrsCache ixCache=getIxCache();
				if(ixCache.hasBeenMarkedSince(fetcher.lastFetched)){
				    ixCache.setTemp(fetcher.theKey.toString(), fetcher.findObject ());
				}
				return (T)ixCache.getTemp(fetcher.theKey.toString());
			}
		},
		/**
		 * look at last indexing, is it older then last time this was called?
		 */
		SUPER_LOCAL_CACHE_WHEN_NOT_CHANGED{
			//for now copy super local eager
			@Override
			<T> T get(EntityFetcher<T> fetcher) throws Exception {
				return LOCAL_EAGER.get(fetcher);
			}
		},
		/**
		 * OLD way (user-specific) (WARNING: BROKEN?)
		 */
		DEFAULT_CACHE{
			@Override
			<T> T get(EntityFetcher<T> fetcher) throws Exception {
				return (T) getIxCache().getOrElse(fetcher.theKey.toString(),() -> fetcher.findObject());
			}
		},
		/**
		 * Store object here, return it directly.
		 */
		ACTIVE_LOAD{
			@Override
			<T> T get(EntityFetcher<T> fetcher) throws Exception {
				return fetcher.getOrReload().get();
			}
		},
		/**
		 * Store object here, right away, return it directly (this is almost what happened before).
		 */
		LOCAL_EAGER {
			@Override
			<T> T get(EntityFetcher<T> fetcher) throws Exception {
				if(getIxCache().hasBeenMarkedSince(fetcher.lastFetched)){
					fetcher.reload();
				}
				return fetcher.stored.get();
			}
		},
		/**
		 * Fetch from:
		 * 1. Cache Ofor JSON
		 * 2. 
		 */
		BACKUP_JSON_EAGER {
			@Override
			<T> T get(EntityFetcher<T> fetcher) throws Exception {				
				if(fetcher.theKey.getEntityInfo().hasBackup()){
				    GsrsCache ixCache=getIxCache();
				    String jkey = fetcher.theKey.toString() +"_JSON";
				    TypedCallable<T> caller = ()->{
                        BackupEntity be = getBackupRepository().getByEntityKey(fetcher.theKey).orElse(null);
                        if(be==null){
                            return GLOBAL_CACHE_WHEN_NOT_CHANGED.get(fetcher);
                        }else{
                            return (T)be.getInstantiated();
                        }
                    };
                    try{
                        if(ixCache.hasBeenMarkedSince(fetcher.lastFetched)){
                            T n = caller.call();
                            fetcher.lastFetched=TimeUtil.getCurrentTimeMillis();
                            ixCache.setTemp(fetcher.theKey.toString(), n);
                        }
                        return ixCache.getOrElseTemp(jkey, caller);
                    }catch(Exception e){
                        return GLOBAL_CACHE_WHEN_NOT_CHANGED.get(fetcher);
                    }
				}
				return GLOBAL_CACHE_WHEN_NOT_CHANGED.get(fetcher);
			}
		};


		 abstract <T> T get(EntityFetcher<T> fetcher) throws Exception;

	}
	public final CacheType cacheType; 
	
	
	final Key theKey;
	
	private Optional<T> stored = Optional.empty(); //
	
	long lastFetched=0l;
	
	public EntityFetcher(Key theKey){
		//this(theKey, CacheType.GLOBAL_CACHE); //This is probably the best option
		this(theKey, CacheType.BACKUP_JSON_EAGER); // This option caches based on
		                                           // raw JSON. This turns out to
		                                           // work pretty well, if not perfectly.
	}
	
	public EntityFetcher(Key theKey, CacheType ct) {
        Objects.requireNonNull(theKey);
        cacheType= ct;
        this.theKey=theKey.toRootKey();
        if(cacheType == CacheType.LOCAL_EAGER){
            reload();
        }
    }
	
	// This can probably be cached without user-specific 
	// concerns
	@Override
	public T call() throws Exception {

		return cacheType.get(this);
	}
	
	public Key getName(){
		return theKey;
	}
	
	public Optional<T> getOrReload(){
		if(stored.isPresent()){
			return stored;
		}else{
			return reload();
		}
	}
	
	public Optional<T> getIfPossible(){
	    try{
	        return Optional.ofNullable(this.call());
	    }catch(Exception e) {
	        return Optional.empty();
	    }
	}
	
	
	//Refresh the "localest" of caches
	public Optional<T> reload() throws NoSuchElementException {
		try{
			stored=Optional.of(findObject());
		}catch(Exception e){
			stored=Optional.empty();
		}
		return stored;
	}
	
	public T findObject () throws NoSuchElementException {
	    lastFetched=TimeUtil.getCurrentTimeMillis();
		return (T) theKey.fetchReadOnlyFull()
		        .get()
		        .getValue();
    }

	public static EntityFetcher<?> of(Key k){
		return new EntityFetcher<>(k);
	}
	
	public static <T> EntityFetcher<T> of(Key k, Class<T> cls) {
		return new EntityFetcher<>(k);
	}
	
	
	public static EntityFetcher<?> of(Key k, CacheType cacheType) {
        return new EntityFetcher<>(k, cacheType);
    }
    
    public static <T> EntityFetcher<T> of(Key k, Class<T> cls,CacheType cacheType) {
        return new EntityFetcher<>(k, cacheType);
    }
    
}
