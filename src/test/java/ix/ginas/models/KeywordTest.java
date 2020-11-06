package ix.ginas.models;

import gsrs.TestUtil;
import ix.core.models.Keyword;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class KeywordTest {

    @Test
    public void create(){
        Keyword keyword = new Keyword("label", "term");
        assertEquals("label", keyword.label);
        assertEquals("term", keyword.term);
        assertNull(keyword.id);

        assertEquals("term", keyword.getValue());

    }

    @Test
    public void equalsAndHashCodeOnlyCheckForLabelAndTerm(){
        Keyword keyword = new Keyword("label", "term");
        Keyword keyword2 = new Keyword("label", "term");

        TestUtil.assertAreEqualsAndHashCodeSame(keyword, keyword2);
        keyword.id= 1234L;
        TestUtil.assertAreEqualsAndHashCodeSame(keyword, keyword2);

        Keyword keyword3 = new Keyword("label3", "term3");
        TestUtil.assertAreNotEqualsAndHashCodeDifferent(keyword, keyword3);
        keyword3.id= 1234L;
        TestUtil.assertAreNotEqualsAndHashCodeDifferent(keyword, keyword3);
    }
}
