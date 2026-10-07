package ix.ginas.models.serialization;

import ix.core.models.Group;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class GroupSerializer extends ValueSerializer<Group> {
    public GroupSerializer () {}

    public void serialize (Group group, JsonGenerator jgen,
                           SerializationContext context)
            throws JacksonException {
        if(group == null || group.name == null){
            jgen.writeNull();
        } else {
            jgen.writeString(group.name);
        }
    }
}
