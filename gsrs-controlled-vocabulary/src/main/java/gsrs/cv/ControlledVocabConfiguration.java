package gsrs.cv;

import gsrs.EnableGsrsApi;
import gsrs.EnableGsrsJpaEntities;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@EnableGsrsJpaEntities
@EnableGsrsApi
@Configuration
public class ControlledVocabConfiguration {
}
