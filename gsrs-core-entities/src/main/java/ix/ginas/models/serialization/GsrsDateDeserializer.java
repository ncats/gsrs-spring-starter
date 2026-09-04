package ix.ginas.models.serialization;

import java.util.Date;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.std.StdDeserializer;

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
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        JsonToken token = jsonParser.currentToken();
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return new Date(jsonParser.getValueAsLong());
        }
        return null;
    }
}
