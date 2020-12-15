package gsrs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ix.core.controllers.EntityFactory;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.lang.reflect.Type;

public class DynamicMappingJacksonHttpMessageConverter extends MappingJackson2HttpMessageConverter {


    @Override
    protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        ObjectMapper mapper = getObjectMapper();

        if(mapper instanceof EntityFactory.EntityMapper){
            System.out.println("entityMapper!!");
            String json = ((EntityFactory.EntityMapper)mapper).toJson(object);
            outputMessage.getBody().write(json.getBytes());
        }
        super.writeInternal(object, type, outputMessage);
    }
}
