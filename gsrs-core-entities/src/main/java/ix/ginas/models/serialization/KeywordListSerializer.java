package ix.ginas.models.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import ix.core.models.Keyword;

import java.io.IOException;
import java.util.List;

public class KeywordListSerializer extends JsonSerializer<List<Keyword>> {
    public KeywordListSerializer () {}
    public void serialize (List<Keyword> keywords, JsonGenerator jgen,
                           SerializerProvider provider)
        throws IOException, JsonProcessingException {
        jgen.writeStartArray();
        //System.out.println("Keywords:" + keywords);
        for (Keyword kw : keywords) {
            provider.defaultSerializeValue(kw.term, jgen);
        }
        jgen.writeEndArray();
    }
}
