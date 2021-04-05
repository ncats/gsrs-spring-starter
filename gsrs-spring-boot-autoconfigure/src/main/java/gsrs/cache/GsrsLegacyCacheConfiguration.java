package gsrs.cache;

import ix.core.cache.IxCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GsrsLegacyCacheConfiguration {
    @Autowired
    private GsrsLegacyCachePropertyConfiguration configuration;

    @Bean
    @ConditionalOnMissingBean
    public IxCache ixCache(){
        /*
        int debugLevel = context.getDebugLevel();

        int maxElements = app.configuration()
        		.getInt(CACHE_MAX_ELEMENTS, DEFAULT_MAX_ELEMENTS);

        int notEvictableMaxElements = app.configuration()
                .getInt(CACHE_MAX_NOT_EVICTABLE_ELEMENTS, DEFAULT_MAX_ELEMENTS);

        int timeToLive = app.configuration()
                .getInt(CACHE_TIME_TO_LIVE, DEFAULT_TIME_TO_LIVE);

        int timeToIdle = app.configuration()
                .getInt(CACHE_TIME_TO_IDLE, DEFAULT_TIME_TO_IDLE);

        GateKeeperFactory.Builder builder = new GateKeeperFactory.Builder( maxElements, timeToLive, timeToIdle)
                .debugLevel(debugLevel)
                .useNonEvictableCache(notEvictableMaxElements,timeToLive,timeToIdle);

        boolean useFileCache = app.configuration().getBoolean(CACHE_USE_FILEDB, true);
        if(useFileCache){
            builder.cacheAdapter(new FileDbCache(context.cache(), "inMemCache"));

        }
        GateKeeper gateKeeper = builder.build().create();
        _instance = new IxCache(gateKeeper);
         */
        return new IxCache(configuration.createNewGateKeeper());

    }
}
