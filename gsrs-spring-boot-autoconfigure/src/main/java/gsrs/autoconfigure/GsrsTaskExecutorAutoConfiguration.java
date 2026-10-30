package gsrs.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Provides the default executor used by GSRS asynchronous operations.
 */
@AutoConfiguration
public class GsrsTaskExecutorAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(SimpleAsyncTaskExecutor.class)
    public SimpleAsyncTaskExecutor gsrsTaskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }
}
