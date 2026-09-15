package ix.ginas.models.serialization;

import ix.ginas.models.v1.FragmentVocabularyTerm;
import ix.ginas.models.v1.VocabularyTerm;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.ArrayList;
import java.util.List;


public class FragmentVocabularyTermListDeserializer extends StdDeserializer<List<VocabularyTerm>> {

    protected FragmentVocabularyTermListDeserializer(Class<?> vc) {
        super(vc);
    }

    public List<VocabularyTerm> deserialize
            (JsonParser parser, DeserializationContext ctx) {

    	List<VocabularyTerm> terms = new ArrayList<>();
        if (parser.currentToken() == JsonToken.START_ARRAY) {
            while (JsonToken.END_ARRAY != parser.nextToken()) {
                VocabularyTerm vt = parser.readValueAs(FragmentVocabularyTerm.class);
                terms.add(vt);
            }
        }
        return terms;
    }
}


