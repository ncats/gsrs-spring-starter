package ix.ginas.models.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Date;

public class GsrsDateSerializer extends StdSerializer<Date> {


    public GsrsDateSerializer() {
        this(null);
    }

    public GsrsDateSerializer(Class t) {
        super(t);
    }

    @Override
    public void serialize (Date value, JsonGenerator gen, SerializerProvider arg2)
            throws IOException {
        if(value ==null){
            gen.writeNull();
        }else {
            gen.writeNumber(value.getTime());
        }
    }
}

