package ix.ginas.models;

import ix.core.models.Keyword;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
public class EmbeddedListTest {

    @Test
    public void create(){
        EmbeddedKeywordList sut = new EmbeddedKeywordList();
        List<Keyword> list = new ArrayList<>();
        list.add( new Keyword("term", "label"));
        list.add( new Keyword("term2", "label2"));
        sut.addAll(list);

        assertEquals(2, sut.size());
        assertEquals(list, sut);
    }

    @Test
    public void neverEquals(){
        EmbeddedKeywordList sut = new EmbeddedKeywordList();
        List<Keyword> list = new ArrayList<>();
        list.add( new Keyword("term", "label"));
        list.add( new Keyword("term2", "label2"));
        sut.addAll(list);

        assertNotEquals(sut, sut);
    }

}
