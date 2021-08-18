package gsrs.cv.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.api.GsrsEntityRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;
import java.util.Optional;

public class ControlledVocabularyRestApi extends GsrsEntityRestTemplate<AbstractGsrsControlledVocabularyDTO, Long> implements ControlledVocabularyApi {
    public ControlledVocabularyRestApi(RestTemplateBuilder restTemplateBuilder, String baseUrl, ObjectMapper mapper) {
        super(restTemplateBuilder, baseUrl, "vocabularies", mapper);
    }

    @Override
    protected AbstractGsrsControlledVocabularyDTO parseFromJson(JsonNode node) {
        return getObjectMapper().convertValue(node, AbstractGsrsControlledVocabularyDTO.class);
    }

    @Override
    protected Long getIdFrom(AbstractGsrsControlledVocabularyDTO dto) {
        return dto.getId();
    }

    @Override
    public Optional<AbstractGsrsControlledVocabularyDTO> findByResolvedId(String anyKindOfId) throws IOException {
        return super.findByResolvedId(anyKindOfId);
    }

    @Override
    public <T extends AbstractGsrsControlledVocabularyDTO> Optional<T> findByDomain(String domain) throws IOException {
        Optional<AbstractGsrsControlledVocabularyDTO> opt= findByResolvedId(domain);
        if(opt.isPresent()){
            return Optional.of((T) opt.get());
        }
        return Optional.empty();
    }

    @Override
    public AbstractGsrsControlledVocabularyDTO create(AbstractGsrsControlledVocabularyDTO dto) throws IOException {
        return super.create(dto);
    }

    @Override
    public AbstractGsrsControlledVocabularyDTO update(AbstractGsrsControlledVocabularyDTO dto) throws IOException {
        return super.update(dto);
    }
}
