package ix.ginas.models.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import ix.core.models.Principal;

import java.io.IOException;

public class PrincipalSerializer extends JsonSerializer<Principal> {
    public PrincipalSerializer () {}
    public void serialize (Principal p, JsonGenerator jgen,
                           SerializerProvider provider)
        throws IOException, JsonProcessingException {
        provider.defaultSerializeValue(p.username, jgen);
    }
}
