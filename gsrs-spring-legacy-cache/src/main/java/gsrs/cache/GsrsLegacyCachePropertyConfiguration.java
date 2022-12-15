package gsrs.cache;

import ix.core.cache.FileDbCache;
import ix.core.cache.GateKeeper;
import ix.core.cache.GateKeeperFactory;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;
@ConfigurationProperties(prefix = "ix.cache", ignoreInvalidFields = true)
@Data
public class GsrsLegacyCachePropertyConfiguration {
  
	
	
	/*
	 * These default values will only be used if there are no corresponding
	 * settings for maxElements, maxElementsNonEvictable, timeToLive and 
	 * timeToIdle in the config file property "ix.cache". To change the 
	 * settings for debugging or for a running instance it's best to
	 * add setting changes to the local.conf like:
	 * 
	 * ix.cache.maxElements = 40
	 * ix.cache.maxElementsNonEvictable = 100
	 * 
	 * and so on
	 */
	
	public static final int DEFAULT_MAX_ELEMENTS = 10000;
    public static final int DEFAULT_TIME_TO_LIVE = 60*60; // 1hr
    public static final int DEFAULT_TIME_TO_IDLE = 60*60; // 1hr

    private int maxElements = DEFAULT_MAX_ELEMENTS;
    private int maxElementsNotEvictable = DEFAULT_MAX_ELEMENTS;
    private int timeToLive = DEFAULT_TIME_TO_LIVE;
    private int timeToIdle = DEFAULT_TIME_TO_IDLE;
    private boolean useFileDb = false;
    
    @Value("${ix.debug:5}")
    private int debugLevel;

    private boolean clearpersist = true;
    private String base;


    public GateKeeper createNewGateKeeper(){
        GateKeeperFactory.Builder builder = new GateKeeperFactory.Builder( maxElements, timeToLive, timeToIdle)
                .debugLevel(debugLevel)
                .useNonEvictableCache(maxElementsNotEvictable,timeToLive,timeToIdle);

        if(useFileDb){
            builder.cacheAdapter(new FileDbCache(base==null? null: new File(base), "inMemCache", clearpersist));

        }
        return  builder.build().create();
    }

}
