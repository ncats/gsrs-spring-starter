package gsrs.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GsrsExportConfigurationTest {

    @Test
    void mapperPropertiesAreIgnoredByExportConfigurationBinding() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ix.ginas.export.mapper.serializer-provider.generator.write-capabilities", "true");
        GsrsExportConfiguration configuration = new GsrsExportConfiguration();

        assertDoesNotThrow(() -> Binder.get(environment)
                .bind("ix.ginas.export", Bindable.ofInstance(configuration)));
    }
}
