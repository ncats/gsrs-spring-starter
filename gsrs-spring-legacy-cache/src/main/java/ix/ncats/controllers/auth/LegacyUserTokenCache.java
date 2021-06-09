package ix.ncats.controllers.auth;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.repository.UserProfileRepository;
import gsrs.security.UserTokenCache;
import ix.core.models.UserProfile;
import ix.utils.Util;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

@Component
public class LegacyUserTokenCache implements UserTokenCache {
	static String CACHE_NAME="TOKEN_CACHE";
	static String CACHE_NAME_UP="TOKEN_UP_CACHE";

	   static CachedSupplier<CacheManager> manager=CachedSupplier.of(()->CacheManager.getInstance());
	   private Cache tokenCache=null;
	   private ConcurrentHashMap<String,UserProfile> tokenCacheUserProfile=new ConcurrentHashMap<String,UserProfile>();
	   private long lastCacheUpdate=-1;

	   private UserProfileRepository userProfileRepository;

		@Autowired
	   public LegacyUserTokenCache(UserProfileRepository userProfileRepository){
	    	this.userProfileRepository = Objects.requireNonNull(userProfileRepository);

	    	//always hold onto the tokens for twice the time required
	    	long tres=Util.getTimeResolutionMS()*2;
	    	
	    	int maxElements=99999;
	        
	        CacheManager manager = LegacyUserTokenCache.manager.get();
	        
	        
	        CacheConfiguration config =
	            new CacheConfiguration (CACHE_NAME, maxElements)
	            	.timeToLiveSeconds(tres/1000);
	        
	        tokenCache = new Cache (config);
	        manager.removeCache(CACHE_NAME);
	        manager.addCache(tokenCache);
			//TODO this isn't in ehcache 2.10 ?
//	        tokenCache.getStatistics().setSampledStatisticsEnabled(true);
	        
	        
	        manager.removeCache(CACHE_NAME_UP);
	        
	        lastCacheUpdate=-1;
	}

	private void updateIfNeeded() {
		if (Util.getCanonicalCacheTimeStamp() != lastCacheUpdate) {
			updateUserProfileTokenCache();
		}
	}

	@Override
	public void updateUserCache(UserProfile up) {
		try{
			up.getRoles();
		//EntityWrapper.of(up).toFullJson();
		
		}catch(Exception e){
			
		}
		String identifier = up.user.username;
		tokenCache.put(new Element(up.getComputedToken(), identifier));
		tokenCacheUserProfile.put(identifier, up);
	}

	@Override
	@Transactional
	public UserProfile getUserProfileFromToken(String token) {
		updateIfNeeded();
		Element e=tokenCache.get(token);
		if(e==null){
			return null;
		}
		return (UserProfile)tokenCacheUserProfile.get(e.getObjectValue());
	}
	
	
	public UserProfile computeUserIfAbsent(String username, Function<String,UserProfile> fetcher){
		UserProfile up= tokenCacheUserProfile.get(username);
		if(up==null){
			up= fetcher.apply(username);
			if(up!=null){
				updateUserCache(up);
			}
			return up;
		}
		return up;
	}
	
	private void updateUserProfileTokenCache(){
	   	//TODO katzelda June 2021 : filter to only active ?
    	try(Stream<UserProfile> stream=userProfileRepository.streamAll()){
    		stream.forEach(up->{
    			updateUserCache(up);
    		});
	    	lastCacheUpdate=Util.getCanonicalCacheTimeStamp();
    	}catch(Exception e){
    		e.printStackTrace();
    		throw e;
    	}
    }	
}
