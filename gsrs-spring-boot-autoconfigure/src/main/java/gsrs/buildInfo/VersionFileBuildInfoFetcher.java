package gsrs.buildInfo;

import gov.nih.ncats.common.util.CachedSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class VersionFileBuildInfoFetcher implements BuildInfoFetcher{

    @Autowired
    private VersionFileBuildInfoFetcherConfiguation config;

    private CachedSupplier<BuildInfo> buildInfoCachedSupplier = CachedSupplier.of(()-> config.getBuildInfo());
    @Override
    public BuildInfo getBuildInfo() {
        return buildInfoCachedSupplier.get();
    }
}
