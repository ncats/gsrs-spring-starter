package gsrs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

public abstract class DynamicMappingJacksonHttpMessageConverter extends MappingJackson2HttpMessageConverter {

    // Spring will override this method with one that provides request scoped bean
    @Override
    public abstract ObjectMapper getObjectMapper();

    @Override
    public void setObjectMapper(ObjectMapper objectMapper) {
        // We dont need that anymore
    }

    public DynamicMappingJacksonHttpMessageConverter() {
        super();
    }

    public DynamicMappingJacksonHttpMessageConverter(ObjectMapper objectMapper) {
        super(objectMapper);
    }
    /* Additionally, you need to override all methods that use objectMapper  attribute and change them to use getObjectMapper() method instead */

}
