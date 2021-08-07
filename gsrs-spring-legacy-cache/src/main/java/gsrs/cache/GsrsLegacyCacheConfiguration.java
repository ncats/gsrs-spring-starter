package gsrs.cache;

import ix.core.cache.IxCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class GsrsLegacyCacheConfiguration {
    @Autowired
    private GsrsLegacyCachePropertyConfiguration configuration;

    public GsrsLegacyCacheConfiguration(){

    }
    public GsrsLegacyCacheConfiguration(GsrsLegacyCachePropertyConfiguration configuration) {
        this.configuration = configuration;
    }
    @Primary
    @Bean
    @ConditionalOnMissingBean(GsrsCache.class)
    public GsrsCache ixCache(){
        return new IxCache(configuration.createNewGateKeeper(), configuration);

    }
}
