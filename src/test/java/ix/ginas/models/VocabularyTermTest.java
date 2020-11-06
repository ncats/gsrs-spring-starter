package ix.ginas.models;

import ix.core.models.Keyword;
import ix.ginas.models.v1.VocabularyTerm;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class VocabularyTermTest {

    @Test
    public void create(){
        VocabularyTerm term = new VocabularyTerm();
        Keyword keyword = new Keyword("label", "term");
        term.filters.add(keyword);

        term.value = "myValue";

        assertEquals("myValue", term.value);
        assertEquals(Arrays.asList(keyword), term.filters);
        assertNull(term.id);
    }
}
