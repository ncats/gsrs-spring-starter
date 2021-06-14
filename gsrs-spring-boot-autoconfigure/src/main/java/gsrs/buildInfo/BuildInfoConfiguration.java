package gsrs.buildInfo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class BuildInfoConfiguration {

    @Bean
    @Order
    @ConditionalOnMissingBean(BuildInfoFetcher.class)
    public BuildInfoFetcher defaultBuildInfoFetcher(){
        return new VersionFileBuildInfoFetcher();
    }

}
