package gsrs.cv.api;

import tools.jackson.databind.JsonNode;
import gsrs.api.GsrsEntityRestTemplate;
import org.springframework.boot.restclient.RestTemplateBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Optional;

public class ControlledVocabularyRestApi extends GsrsEntityRestTemplate<AbstractGsrsControlledVocabularyDTO, Long> implements ControlledVocabularyApi {
    public ControlledVocabularyRestApi(RestTemplateBuilder restTemplateBuilder, String baseUrl, JsonMapper mapper) {
        super(restTemplateBuilder, baseUrl, "vocabularies", mapper);
    }

    @Override
    protected AbstractGsrsControlledVocabularyDTO parseFromJson(JsonNode node) {
        return getMapper().convertValue(node, AbstractGsrsControlledVocabularyDTO.class);
    }

    @Override
    protected Long getIdFrom(AbstractGsrsControlledVocabularyDTO dto) {
        return dto.getId();
    }


    @Override
    public <T extends AbstractGsrsControlledVocabularyDTO> Optional<T> findByDomain(String domain) throws IOException {
        Optional<AbstractGsrsControlledVocabularyDTO> opt= findByResolvedId(domain);
        return opt.map(abstractGsrsControlledVocabularyDTO -> (T) abstractGsrsControlledVocabularyDTO);
    }
}
