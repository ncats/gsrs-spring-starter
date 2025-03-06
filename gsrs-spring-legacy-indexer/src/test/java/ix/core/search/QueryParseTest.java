package ix.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.junit.jupiter.api.Test;

import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexer.IxQueryParser;

public class QueryParseTest {

    private static final Pattern QUOTES_AROUND_WORD_REMOVER = Pattern
            .compile("\"([^\" .=-]*)\"");
    final static String[] BREAK_TOKENS = new String[] {
            " ", ".", "-", "="      
          };

    @Test
    public void quotesAroundWordShouldBeRemoved() throws Exception{
        
        assertEquals("*test*", QUOTES_AROUND_WORD_REMOVER.matcher("\"*test*\"").replaceAll("$1"));
        assertEquals("\"*another test*\" *test*", QUOTES_AROUND_WORD_REMOVER.matcher("\"*another test*\" \"*test*\"").replaceAll("$1"));
        assertEquals("*test3* *test*", QUOTES_AROUND_WORD_REMOVER.matcher("\"*test3*\" \"*test*\"").replaceAll("$1"));
        assertEquals("simple", QUOTES_AROUND_WORD_REMOVER.matcher("\"simple\"").replaceAll("$1"));
        assertEquals("\"multi word\"", QUOTES_AROUND_WORD_REMOVER.matcher("\"multi word\"").replaceAll("$1"));
    }
    

    @Test
    public void quotesAroundWordWithHyphenShouldNotBeRemoved() throws Exception{
        
        assertEquals("\"*another-test*\" *test*", QUOTES_AROUND_WORD_REMOVER.matcher("\"*another-test*\" \"*test*\"").replaceAll("$1"));
        assertEquals("*test3* *test*", QUOTES_AROUND_WORD_REMOVER.matcher("\"*test3*\" \"*test*\"").replaceAll("$1"));
        assertEquals("\"multi-word\"", QUOTES_AROUND_WORD_REMOVER.matcher("\"multi-word\"").replaceAll("$1"));
        assertEquals("\"50-00-0\"", QUOTES_AROUND_WORD_REMOVER.matcher("\"50-00-0\"").replaceAll("$1"));
    }
    
    @Test
    public void quotesAroundCommonTokenizedWordCharactersRemain() throws Exception{
        
        
        for(String t: BREAK_TOKENS) {
            assertEquals("\"*another" + t + "test*\" *test*", QUOTES_AROUND_WORD_REMOVER.matcher("\"*another" + t +"test*\" \"*test*\"").replaceAll("$1"));
            
        }
    }
    
    @Test
    public void confirmSimpleWildcardQueryIsNotComplex() throws Exception{
        IxQueryParser iqp = new TextIndexer.IxQueryParser("text");
        Query q = iqp.parse("Brentuxima*");
        assertTrue("Simple prefix query should be prefix query", q instanceof PrefixQuery);
        assertEquals("text:brentuxima*", q.toString());
    }
    

    @Test
    public void confirmSimpleWildcardContainsQueryIsNotComplex() throws Exception{
        IxQueryParser iqp = new TextIndexer.IxQueryParser("text");
        Query q = iqp.parse("*rentuxima*");
        assertTrue("Simple contains query should be wildcard query", q instanceof WildcardQuery);
        assertEquals("text:*rentuxima*", q.toString());
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
    

    @Test
    public void confirmWildcardContainsQueryParsedAsComplexPhraseQuery() throws Exception{
        IxQueryParser iqp = new TextIndexer.IxQueryParser("text");
        Query q = iqp.parse("text:\"*rentuximab Vedoti*\"");
        
        assertTrue("Complex phrase query should NOT be phrase query", !(q instanceof PhraseQuery));
        assertTrue("Complex phrase query should contain *", q.toString().contains("*rentuximab Vedoti*"));
        assertEquals("Complex phrase query ComplexPhraseQuery Object", "org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser$ComplexPhraseQuery",q.getClass().getName());
    }
    

    @Test
    public void confirmWildcardContainsQueryWithOneWordAndQuotesParsedAsWildcardQuery() throws Exception{
        IxQueryParser iqp = new TextIndexer.IxQueryParser("text");
        Query q = iqp.parse("text:\"*rentuxima*\"");
        
        assertTrue("Simple contains query wrapped in quotes should be wildcard query", q instanceof WildcardQuery);
        assertEquals("text:*rentuxima*", q.toString());
    }
    

    @Test
    public void confirmWildcardContainsQueryWithOneBreakCharAndQuotesParsedAsComplexPhraseQuery() throws Exception{
        IxQueryParser iqp = new TextIndexer.IxQueryParser("text");

        for(String t: BREAK_TOKENS) {
            Query q = iqp.parse("text:\"*rentu" + t + "xima*\"");
            System.out.println(t);
            assertTrue("Complex phrase query should NOT be phrase query", !(q instanceof PhraseQuery));
            assertTrue("Complex phrase query should contain *", q.toString().contains("*rentu" + t + "xima*"));
            assertEquals("Complex phrase query ComplexPhraseQuery Object", "org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser$ComplexPhraseQuery",q.getClass().getName());
        }
       
    }
    

    @Test
    public void replaceSpaceButNotEscapedQuoteInReplaceStringInComplexPhraseQuery() {

        assertEquals("\"*testwith\\\"escapedquote_XSPCX_works\"",
                TextIndexer.preProcessQueryText("\"*testwith\\\"escapedquote works\""));
    }
    @Test
    public void replaceDashAndSpaceWithReplaceStringInComplexPhraseQuery() {

        
    	assertEquals("*OAT-2*", TextIndexer.preProcessQueryText("*OAT-2*"));
    	assertEquals("OAT-2", TextIndexer.preProcessQueryText("OAT-2"));
    	
    	assertEquals("\"*OAT_XSPCX_2*\"", TextIndexer.preProcessQueryText("\"*OAT.2*\""));
    	assertEquals("\"*OAT_XSPCX_2*\"", TextIndexer.preProcessQueryText("\"*OAT&2*\""));    	
    	assertEquals("\"*OAT_XSPCX_2\"", TextIndexer.preProcessQueryText("\"*OAT-2\""));
    	assertEquals("\"OAT_XSPCX_2*\"", TextIndexer.preProcessQueryText("\"OAT 2*\""));
    	    	
    	assertEquals("root_names_name:\"aspirin sodium\" AND \"*TEST123_XSPCX_456*\"", 
    			TextIndexer.preProcessQueryText("root_names_name:\"aspirin sodium\" AND \"*TEST123 456*\""));
    	
    	assertEquals("root_names_name:\"*aspirin_XSPCX_sodium*\" AND \"*TEST123_XSPCX_456*\"", 
    			TextIndexer.preProcessQueryText("root_names_name:\"*aspirin sodium*\" AND \"*TEST123 456*\""));
    	
    	assertEquals("\"*OAT_XSPCX_2_XSPCX_BETA_XSPCX_*\"", TextIndexer.preProcessQueryText("\"*OAT-2β*\""));
    	assertEquals("\"*OAT_XSPCX__XSPCX__XSPCX_2*\"", TextIndexer.preProcessQueryText("\"*OAT-.&2*\""));
    	
    	assertEquals("root_names_name:\"abc_XSPCX__XSPCX_BETA_XSPCX_def*\"", 
    			TextIndexer.preProcessQueryText("root_names_name:\"abc-βdef*\""));
    	
    	assertEquals("\"*TEST123_XSPCX_456*\"", TextIndexer.preProcessQueryText("\"*TEST123 456*\""));
    	
    	assertEquals("\"*TEST123_XSPCX_456\" and root_lastEdited:[-10E50 TO 10E50]", 
    			TextIndexer.preProcessQueryText("\"*TEST123 456\" and root_lastEdited:[-10E50 TO 10E50]"));
    	
    	assertEquals("\"*TEST123_XSPCX_456*\" AND root_names_name:\"aspirin sodium\"", 
    			TextIndexer.preProcessQueryText("\"*TEST123 456*\" AND root_names_name:\"aspirin sodium\""));
    	
    	assertEquals("root_names_name:\"OCT*\"", TextIndexer.preProcessQueryText("root_names_name:\"OCT*\""));
    	assertEquals("root_names_name:\"abc*\" AND def*", TextIndexer.preProcessQueryText("root_names_name:\"abc*\" AND def*"));
    	
    	assertEquals("root_names_name:\"OAT_XSPCX_2*\" AND abc-def*", 
    			TextIndexer.preProcessQueryText("root_names_name:\"OAT-2*\" AND abc-def*"));
    	
    	assertEquals("root_names_name:\"*OCT_XSPCX_1*\"", TextIndexer.preProcessQueryText("root_names_name:\"*OCT-1*\""));
    	assertEquals("root_names_name:  \"*OCT_XSPCX_123*\"", TextIndexer.preProcessQueryText("root_names_name:  \"*OCT&123*\""));
    	
    	assertEquals("root_names_name:\"*OCT_XSPCX_1*\" AND   root_codes_code:\"*OCT_XSPCX_2*\" OR root_approvalID:\"*OCT_XSPCX_3*\"", 
    			TextIndexer.preProcessQueryText("root_names_name:\"*OCT-1*\" AND   root_codes_code:\"*OCT-2*\" OR root_approvalID:\"*OCT-3*\""));    	
    	
    	assertEquals("(root_names_name:\"*OCT_XSPCX_2*\" AND root_codes_code:\"*OCT_XSPCX_2*\") OR (root_approvalID:\"*OCT_XSPCX_3*\" AND root_references_citation:\"*OCT_XSPCX_4*\")",
    			TextIndexer.preProcessQueryText("(root_names_name:\"*OCT 2*\" AND root_codes_code:\"*OCT 2*\") OR (root_approvalID:\"*OCT 3*\" AND root_references_citation:\"*OCT-4*\")"));
    	
    	
    	    	    	
    }
    
    @Test
    public void replaceForwardSlashWithWhitespsceInExactSearchQuery() {
    	assertEquals("root_root_names:\"california 7 2009\" AND root_codes_comments:\"hello code comments\"",
                TextIndexer.preProcessQueryText("root_root_names:\"california\\/7\\/2009\" AND root_codes_comments:\"hello\\/code\\/comments\""));
    }
}
