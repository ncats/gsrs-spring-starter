package ix.ginas.models.serialization;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Date;

public class GsrsDateSerializer extends StdSerializer<Date> {


    public GsrsDateSerializer() {
        this(null);
    }

    public GsrsDateSerializer(Class t) {
        super(t);
    }

    @Override
    public void serialize (Date value, JsonGenerator gen, SerializationContext context)
            throws JacksonException {
        if(value ==null){
            gen.writeNull();
        }else {
            gen.writeNumber(value.getTime());
        }
    }
}

