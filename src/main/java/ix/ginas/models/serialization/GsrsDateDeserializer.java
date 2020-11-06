package ix.ginas.models.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Date;

public class GsrsDateDeserializer extends StdDeserializer<Date> {
    public GsrsDateDeserializer(){
        super((Class) null);
    }
    public GsrsDateDeserializer(Class<?> vc) {
        super(vc);
    }

    public GsrsDateDeserializer(JavaType valueType) {
        super(valueType);
    }

    public GsrsDateDeserializer(StdDeserializer<?> src) {
        super(src);
    }

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        JsonToken token = jsonParser.getCurrentToken();
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return new Date(jsonParser.getValueAsLong());
        }
        return null;
    }
}
