package ix.ncats.controllers.auth;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.repository.UserProfileRepository;
import gsrs.security.UserTokenCache;
import gsrs.security.TokenConfiguration;
import ix.core.models.UserProfile;
import ix.utils.Util;
import jdk.nashorn.internal.parser.Token;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

@Component
public class LegacyUserTokenCache implements UserTokenCache {
		private static String CACHE_NAME="TOKEN_CACHE";
		private static String CACHE_NAME_UP="TOKEN_UP_CACHE";

	   static CachedSupplier<CacheManager> manager=CachedSupplier.of(()->CacheManager.getInstance());
	   private Cache tokenCache=null;
	   private ConcurrentHashMap<String,UserProfile> tokenCacheUserProfile=new ConcurrentHashMap<>();
	   private long lastCacheUpdate=-1;

	   private UserProfileRepository userProfileRepository;

	   
       private TokenConfiguration tokenConfiguration;

	   @Autowired
	   public LegacyUserTokenCache(UserProfileRepository userProfileRepository, TokenConfiguration tokenConfiguration ){
	    	this.userProfileRepository = Objects.requireNonNull(userProfileRepository);

	    	//always hold onto the tokens for twice the time required
		   long tres = tokenConfiguration.timeResolutionMS()*2;

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
		if (tokenConfiguration.getCanonicalCacheTimeStamp() != lastCacheUpdate) {
			updateUserProfileTokenCache();
		}
	}

	private void updateUserCache(UserProfileRepository.UserTokenInfo info){
			String identifier = info.getUsername();
		tokenCache.put(new Element(tokenConfiguration.getComputedToken(info.getUsername(), info.getKey()), identifier));
		//TODO commented out moved to a computeIfAbsent
//		tokenCacheUserProfile.put(identifier, up);
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
	public void evictUser(UserProfile up) {
		updateIfNeeded();
		tokenCache.remove(up.getComputedToken());
		tokenCacheUserProfile.remove(up.user.username);
	}

	@Override
	@Transactional
	public UserProfile getUserProfileFromToken(String token) {
		updateIfNeeded();
		Element e=tokenCache.get(token);
		if(e==null){
			return null;
		}
		return (UserProfile)tokenCacheUserProfile.computeIfAbsent((String) e.getObjectValue(),
				username -> {
				    return Optional.ofNullable(userProfileRepository.findByUser_UsernameIgnoreCase(username))
                    .map(oo->oo.standardize())
                    .orElse(null);
				}
				);
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
//    	try(Stream<UserProfile> stream=userProfileRepository.streamAll()){
//    		stream.forEach(up->{
//    			updateUserCache(up);
//    		});
//	    	lastCacheUpdate=Util.getCanonicalCacheTimeStamp();
//    	}catch(Exception e){
//    		e.printStackTrace();
//    		throw e;
//    	}

    	try(Stream<UserProfileRepository.UserTokenInfo> stream = userProfileRepository.streamAllTokenInfo()){
    		stream.forEach(info->{
    			updateUserCache(info);
			});
			lastCacheUpdate=tokenConfiguration.getCanonicalCacheTimeStamp();
		}
    }	
}
