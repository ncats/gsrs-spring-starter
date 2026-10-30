package gsrs.coretests;

import gsrs.GSRSDataSourceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GSRSDataSourceConfigTest {

    private final TestDataSourceConfig config = new TestDataSourceConfig();

    @Test
    void additionalJpaPropertiesDefaultsMaxFetchDepthToZero() {
        Map<String, ?> properties = config.additionalJpaProperties("spring");

        assertEquals("0", properties.get("hibernate.max_fetch_depth"));
    }

    @Test
    void additionalJpaPropertiesUsesGenericMaxFetchDepthOverride() {
        ReflectionTestUtils.setField(config, "env",
                new MockEnvironment().withProperty("spring.jpa.properties.hibernate.max_fetch_depth", "2"));

        Map<String, ?> properties = config.additionalJpaProperties("spring");

        assertEquals("2", properties.get("hibernate.max_fetch_depth"));
    }

    @Test
    void additionalJpaPropertiesUsesDataSourceSpecificMaxFetchDepthOverrideFirst() {
        ReflectionTestUtils.setField(config, "env",
                new MockEnvironment()
                        .withProperty("custom.jpa.properties.hibernate.max_fetch_depth", "1")
                        .withProperty("spring.jpa.properties.hibernate.max_fetch_depth", "2"));

        Map<String, ?> properties = config.additionalJpaProperties("custom");

        assertEquals("1", properties.get("hibernate.max_fetch_depth"));
    }

    private static class TestDataSourceConfig extends GSRSDataSourceConfig {
    }
}