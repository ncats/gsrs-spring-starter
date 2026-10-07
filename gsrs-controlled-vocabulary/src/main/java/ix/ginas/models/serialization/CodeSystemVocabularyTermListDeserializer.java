package ix.ginas.models.serialization;

import ix.ginas.models.v1.CodeSystemVocabularyTerm;
import ix.ginas.models.v1.VocabularyTerm;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.core.JsonParser;

import java.util.ArrayList;
import java.util.List;


public class CodeSystemVocabularyTermListDeserializer extends StdDeserializer<List<VocabularyTerm>> {

    public CodeSystemVocabularyTermListDeserializer() {
        this(List.class);
    }

    protected CodeSystemVocabularyTermListDeserializer(Class<?> vc) {
        super(vc);
    }

    public List<VocabularyTerm> deserialize
            (JsonParser parser, DeserializationContext ctx) {

    	List<VocabularyTerm> terms = new ArrayList<VocabularyTerm>();
        if (parser.currentToken() == JsonToken.START_ARRAY) {
            while (JsonToken.END_ARRAY != parser.nextToken()) {
                VocabularyTerm vt = parser.readValueAs(CodeSystemVocabularyTerm.class);
                terms.add(vt);
            }
        }
        return terms;
    }
}


