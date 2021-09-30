package gsrs.cv;

import gsrs.EnableGsrsApi;
import gsrs.EnableGsrsJpaEntities;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.legacy.DefaultReindexService;
import gsrs.legacy.ReindexService;
import gsrs.repository.ControlledVocabularyRepository;
import ix.ginas.models.v1.ControlledVocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;


@EnableGsrsJpaEntities
@EnableGsrsApi
@Configuration
public class ControlledVocabConfiguration {


    @Bean
    @ConditionalOnMissingBean(ControlledVocabularyApi.class)
    @Order()
    public ControlledVocabularyApi controlledVocabularyApi(@Autowired  ControlledVocabularyEntityService service){
        return new CvApiAdapter(service);
    }

    @Bean
    public ReindexService<ControlledVocabulary> cvReindexService(ControlledVocabularyRepository repository){
        return new DefaultReindexService<>(repository, ControlledVocabulary.class);
    }
}
