package ix.ginas.models.serialization;

import ix.core.models.Principal;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class PrincipalSerializer extends ValueSerializer<Principal> {
    public PrincipalSerializer () {}
    public void serialize (Principal p, JsonGenerator jgen,
                           SerializationContext context)
        throws JacksonException {
        if (p == null || p.username == null) {
            jgen.writeNull();
        } else {
            jgen.writeString(p.username);
        }
    }
}
