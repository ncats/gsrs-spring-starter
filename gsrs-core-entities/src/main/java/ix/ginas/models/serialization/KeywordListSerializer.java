package ix.ginas.models.serialization;

import ix.core.models.Keyword;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.List;

public class KeywordListSerializer extends ValueSerializer<List<Keyword>> {
    public KeywordListSerializer () {}
    public void serialize (List<Keyword> keywords, JsonGenerator jgen,
                           SerializationContext context)
        throws JacksonException {
        jgen.writeStartArray();
        //System.out.println("Keywords:" + keywords);
        for (Keyword kw : keywords) {
            if (kw == null || kw.term == null) {
                jgen.writeNull();
            } else {
                jgen.writeString(kw.term);
            }
        }
        jgen.writeEndArray();
    }
}
