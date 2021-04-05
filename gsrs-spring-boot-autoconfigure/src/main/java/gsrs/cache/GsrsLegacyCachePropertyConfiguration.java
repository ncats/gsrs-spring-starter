package gsrs.cache;

import ix.core.cache.FileDbCache;
import ix.core.cache.GateKeeper;
import ix.core.cache.GateKeeperFactory;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

@ConfigurationProperties("ix.cache")
@Data
public class GsrsLegacyCachePropertyConfiguration {
    /*

    static final int DEFAULT_MAX_ELEMENTS = 10000;
    static final int DEFAULT_TIME_TO_LIVE = 60*60; // 1hr
    static final int DEFAULT_TIME_TO_IDLE = 60*60; // 1hr

    public static final String CACHE_MAX_ELEMENTS = "ix.cache.maxElements";
    public static final String CACHE_MAX_NOT_EVICTABLE_ELEMENTS = "ix.cache.maxElementsNotEvictable";
    public static final String CACHE_TIME_TO_LIVE = "ix.cache.timeToLive";
    public static final String CACHE_TIME_TO_IDLE = "ix.cache.timeToIdle";

    public static final String CACHE_USE_FILEDB = "ix.cache.useFileDb";
     */
    public static final int DEFAULT_MAX_ELEMENTS = 10000;
    public static final int DEFAULT_TIME_TO_LIVE = 60*60; // 1hr
    public static final int DEFAULT_TIME_TO_IDLE = 60*60; // 1hr

    int maxElements = DEFAULT_MAX_ELEMENTS;
    int maxElementsNotEvictable = DEFAULT_MAX_ELEMENTS;
    int timeToLive = DEFAULT_TIME_TO_LIVE;
    int timeToIdle = DEFAULT_TIME_TO_IDLE;
    boolean useFileDb = true;
    @Value(("${ix.debug:2"))
    int debugLevel;

    boolean clearpersist = false;
    public File base;


    public GateKeeper createNewGateKeeper(){
        GateKeeperFactory.Builder builder = new GateKeeperFactory.Builder( maxElements, timeToLive, timeToIdle)
                .debugLevel(debugLevel)
                .useNonEvictableCache(maxElementsNotEvictable,timeToLive,timeToIdle);

        if(useFileDb){
            builder.cacheAdapter(new FileDbCache(base, "inMemCache", clearpersist));

        }
        return  builder.build().create();
    }

}
