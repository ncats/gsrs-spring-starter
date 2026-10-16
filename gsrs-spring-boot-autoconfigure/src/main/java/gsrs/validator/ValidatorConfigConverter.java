package gsrs.validator;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConfigurationPropertiesBinding
public class ValidatorConfigConverter implements Converter<String, ValidatorConfigList> {
    private final static JsonMapper mapper = JsonMapper.builderWithJackson2Defaults()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Override
    public ValidatorConfigList convert(String s) {
        try {
            JsonNode node = mapper.readTree(s);

            return mapper.treeToValue(node, ValidatorConfigList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
