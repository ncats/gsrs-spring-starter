package gsrs.cv;

import gsrs.EnableGsrsApi;
import gsrs.EnableGsrsJpaEntities;
import gsrs.cv.api.ControlledVocabularyApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.json.JsonMapper;


@EnableGsrsJpaEntities
@EnableGsrsApi
@Configuration
public class ControlledVocabConfiguration {


    @Bean
    @ConditionalOnMissingBean(ControlledVocabularyApi.class)
    @Order()
    public ControlledVocabularyApi controlledVocabularyApi(@Autowired  ControlledVocabularyEntityService service,
                                                           @Qualifier("legacyJsonMapper") JsonMapper jsonMapper){
        return new CvApiAdapter(service, jsonMapper);
    }
}
