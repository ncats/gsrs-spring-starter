package ix.ginas.models.serialization;

import ix.core.models.Keyword;
import ix.ginas.models.GinasCommonData;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class KeywordDeserializer extends ValueDeserializer<Keyword> {
	
    private final String label;
    public KeywordDeserializer (String label) {
    	
        this.label = label;
    }
    
    public KeywordDeserializer () {
        this.label = null;
    }

    public Keyword deserialize
        (JsonParser parser, DeserializationContext ctx)
        throws JacksonException {
        Keyword kw=null;
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_STRING) {
        	kw= new Keyword(label, parser.getValueAsString());
        }
        return kw;
    }
    
    //kept getter package-private to keep it
    //the same as when we used direct field access
    String getLabel() {
        return label;
    }
    
    public static class LanguageDeserializer extends KeywordDeserializer {
        public LanguageDeserializer () {
            super (GinasCommonData.LANGUAGE);
        }
    }
    public static class DomainDeserializer extends KeywordDeserializer {
        public DomainDeserializer () {
            super (GinasCommonData.DOMAIN);
        }
    }
    public static class ReferenceTagDeserializer extends KeywordDeserializer {
        public ReferenceTagDeserializer () {
            super (GinasCommonData.REFERENCE_TAG);
        }
    }
    public static class TagDeserializer extends KeywordDeserializer {
        public TagDeserializer () {
            super (GinasCommonData.TAG);
        }
    }
    public static class PartDeserializer extends KeywordDeserializer {
        public PartDeserializer () {
            super ("Parts");
        }
    }
    public static class JurisdictionDeserializer extends KeywordDeserializer {
        public JurisdictionDeserializer () {
            super (GinasCommonData.NAME_JURISDICTION);
        }
    }
    public static class SubClassDeserializer extends KeywordDeserializer {
        public SubClassDeserializer () {
            super (GinasCommonData.SUB_CLASS);
        }
    }
    
}
