package ix.core.search.text;

import static org.junit.Assert.assertEquals;
import org.junit.jupiter.api.Test;


public class ColonIndexedTextEncoderTest {
    private static final String TEST_COLON_WORD = "XCOLONX";

    @Test
    public void basicEncoderTest() throws Exception {
        ColonIndexedTextEncoder cite = new ColonIndexedTextEncoder();
        assertEquals(cite.encode("ABC : DEF"), "ABC " + TEST_COLON_WORD +  " DEF");
        assertEquals(cite.encode("ABC:DEF"), "ABC" + TEST_COLON_WORD +  "DEF");
        // Should not do anything with quotes at indexing time.
        assertEquals(cite.encode("\"ABC : DEF\""), "\"ABC " + TEST_COLON_WORD +  " DEF\"");
    }

    @Test
    public void basicEncodeQueryTest() throws Exception {
        ColonIndexedTextEncoder cite = new ColonIndexedTextEncoder();
        assertEquals(cite.encodeQuery("\"ABC : DEF\""), "\"ABC " + TEST_COLON_WORD +  " DEF\"");
        assertEquals(cite.encodeQuery("\"ABC:DEF\""), "\"ABC" + TEST_COLON_WORD +  "DEF\"");
        String stringA = "root_names_name:\"AZT : ABC\" AND root_names_name:\"AZT : DEF\"";
        String resultA = String.format("root_names_name:\"AZT %s ABC\" AND root_names_name:\"AZT %s DEF\"", TEST_COLON_WORD, TEST_COLON_WORD);
        assertEquals(cite.encodeQuery(stringA), resultA);
        // Escaped quotes should be conserved. The encodeQuery method handles them differently than
        // non-escaped quotes.
        String stringB = "\\\"root_names_name:\"AZT : ABC\"\\\"";
        String resultB = String.format("\\\"root_names_name:\"AZT %s ABC\"\\\"", TEST_COLON_WORD);
        assertEquals(cite.encodeQuery(stringB), resultB);
    }
}
