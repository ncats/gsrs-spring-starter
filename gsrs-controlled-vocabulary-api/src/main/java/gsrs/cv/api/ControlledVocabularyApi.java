package gsrs.cv.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.api.GsrsEntityRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.List;

public class ControlledVocabularyApi extends GsrsEntityRestTemplate<AbstractGsrsControlledVocabularyDTO, Long> {
    public ControlledVocabularyApi(RestTemplateBuilder restTemplateBuilder, String baseUrl, ObjectMapper mapper) {
        super(restTemplateBuilder, baseUrl, "vocabularies", mapper);
    }

    @Override
    protected AbstractGsrsControlledVocabularyDTO parseFromJson(JsonNode node) {
        return getObjectMapper().convertValue(node, AbstractGsrsControlledVocabularyDTO.class);
    }


}
