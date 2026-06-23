package gsrs.buildInfo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class VersionFileBuildInfoFetcherConfiguationTest {

    @Test
    void buildInfoFlagDoesNotBindToGeneratedBuildInfo() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("build.build-info", "true");
        VersionFileBuildInfoFetcherConfiguation configuration = new VersionFileBuildInfoFetcherConfiguation();

        assertDoesNotThrow(() -> Binder.get(environment)
                .bind("build", Bindable.ofInstance(configuration)));
    }
}
