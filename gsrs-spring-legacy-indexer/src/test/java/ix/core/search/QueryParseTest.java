package ix.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.Test;

import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexer.IxQueryParser;

public class QueryParseTest {
    
    @Test
    public void confirmSimpleWildcardQueryIsNotComplex() throws Exception{
        IxQueryParser iqp = new TextIndexer.IxQueryParser("text");
        Query q = iqp.parse("Brentuxima*");
        assertTrue("Simple prefix query should be prefix query", q instanceof PrefixQuery);
        assertEquals("text:brentuxima*", q.toString());
    }
    
    @Test
    public void confirmSimpleTermQueryIsNotComplex() throws Exception{
        IxQueryParser iqp = new TextIndexer.IxQueryParser("text");
        Query q = iqp.parse("Brentuximab");
        assertTrue("Simple term query should be term query", q instanceof TermQuery);
        assertEquals("text:brentuximab", q.toString());
    }
    
    
    
    
    @Test
    public void confirmSimplePhraseQueryParsedAsPhraseQuery() throws Exception{
        IxQueryParser iqp = new TextIndexer.IxQueryParser("text");
        Query q = iqp.parse("\"Brentuximab Vedotin\"");
        assertTrue("Simple phrase query should be phrase query", q instanceof PhraseQuery);
        assertEquals("text:\"brentuximab vedotin\"", q.toString());
    }
    
    @Test
    public void confirmWildcardPhraseQueryParsedAsComplexPhraseQuery() throws Exception{
        IxQueryParser iqp = new TextIndexer.IxQueryParser("text");
        Query q = iqp.parse("text:\"Brentuximab Vedoti*\"");
        
        assertTrue("Complex phrase query should NOT be phrase query", !(q instanceof PhraseQuery));
        assertTrue("Complex phrase query should contain *", q.toString().contains("Brentuximab Vedoti*"));
        assertEquals("Complex phrase query ComplexPhraseQuery Object", "org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser$ComplexPhraseQuery",q.getClass().getName());
    }
}
