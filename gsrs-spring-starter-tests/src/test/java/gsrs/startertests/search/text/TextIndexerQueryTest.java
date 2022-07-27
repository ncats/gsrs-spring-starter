package gsrs.startertests.search.text;

import static org.apache.lucene.document.Field.Store.NO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gsrs.GsrsSpringApplication;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;

@ActiveProfiles("test")
@GsrsJpaTest( classes = { GsrsSpringApplication.class, GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class})
public class TextIndexerQueryTest extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    private TextIndexerFactory tif;
    
    private TextIndexer indexer;
    
    @BeforeEach
    public void setup() throws Exception {
        indexer = tif.getDefaultInstance();
      
    }
    
    @AfterEach
    public void teardown() throws Exception {
        indexer.remove(new MatchAllDocsQuery());
    }
    
   
    
    @Test
    public void confirmWildcardPhraseQueryGetsPhraseResults() throws Exception{
        
        Document doc1 = new Document();
        doc1.add(new TextField("text", "brentuximab vedotin", NO));
        indexer.addDoc(doc1);
        Document doc2 = new Document();
        doc2.add(new TextField("text", "brentuximab vedoton", NO));
        indexer.addDoc(doc2);
        Document doc3 = new Document();
        doc3.add(new TextField("text", "brentuximab vevoton", NO));
        indexer.addDoc(doc3);
        Document doc4 = new Document();
        doc4.add(new TextField("text", "brentuximad vevoton", NO));
        indexer.addDoc(doc4);
        Document doc5 = new Document();
        doc5.add(new TextField("code", "brentuximab vedotin", NO));
        indexer.addDoc(doc5);
        Document doc6 = new Document();
        doc6.add(new TextField("text", indexer.toExactMatchString("prentuximab vedoton"), NO));
        indexer.addDoc(doc6);
        
        Query q = indexer.getQueryParser().parse("\"brentuximab vedot*\"");
        

        SearchResult sr=SearchResult.createBuilder().build();
        TopDocs hits = indexer.withSearcher(searcher->{
            try (TaxonomyReader taxon = new DirectoryTaxonomyReader(indexer.getTaxonWriter())) {
                return indexer.firstPassLuceneSearch(searcher,taxon,sr,null, q);
            }
        });

        assertEquals(2, hits.totalHits);

    }
    
    @Test
    public void confirmStartsWithPhraseWildcardGetsPhraseResults() throws Exception{

        Document doc1 = new Document();
        doc1.add(new TextField("text", indexer.toExactMatchString("brentuximab vedotin"), NO));
        indexer.addDoc(doc1);
        Document doc2 = new Document();
        doc2.add(new TextField("text", indexer.toExactMatchString("brentuximab vedoton"), NO));
        indexer.addDoc(doc2);
        Document doc3 = new Document();
        doc3.add(new TextField("text", indexer.toExactMatchString("brentuximab vevoton"), NO));
        indexer.addDoc(doc3);
        Document doc4 = new Document();
        doc4.add(new TextField("text", indexer.toExactMatchString("brentuximad vevoton"), NO));
        indexer.addDoc(doc4);
        Document doc5 = new Document();
        doc5.add(new TextField("text", indexer.toExactMatchString("not actually brentuximab vedotin"), NO));
        indexer.addDoc(doc5);
        Document doc6 = new Document();
        doc6.add(new TextField("text", indexer.toExactMatchString("prentuximab vedoton"), NO));
        indexer.addDoc(doc6);
        
        Query q = indexer.getQueryParser().parse("\"^brentuximab vedot*\"");
        

        SearchResult sr=SearchResult.createBuilder().build();
        TopDocs hits = indexer.withSearcher(searcher->{
            try (TaxonomyReader taxon = new DirectoryTaxonomyReader(indexer.getTaxonWriter())) {
                return indexer.firstPassLuceneSearch(searcher,taxon,sr,null, q);
            }
        });

        assertEquals(2, hits.totalHits);

    }
    

    @Test
    public void confirmContainsPhraseWildcardGetsPhraseResults() throws Exception{

        Document doc1 = new Document();
        doc1.add(new TextField("text", indexer.toExactMatchString("brentuximab vedotin"), NO));
        indexer.addDoc(doc1);
        Document doc2 = new Document();
        doc2.add(new TextField("text", indexer.toExactMatchString("brentuximab vedoton"), NO));
        indexer.addDoc(doc2);
        Document doc3 = new Document();
        doc3.add(new TextField("text", indexer.toExactMatchString("brentuximab vevoton"), NO));
        indexer.addDoc(doc3);
        Document doc4 = new Document();
        doc4.add(new TextField("text", indexer.toExactMatchString("brentuximad vevoton"), NO));
        indexer.addDoc(doc4);
        Document doc5 = new Document();
        doc5.add(new TextField("text", indexer.toExactMatchString("not actually brentuximab vedotin"), NO));
        indexer.addDoc(doc5);
        
        Document doc6 = new Document();
        doc6.add(new TextField("text", indexer.toExactMatchString("prentuximab vedoton"), NO));
        indexer.addDoc(doc6);
        
        Query q = indexer.getQueryParser().parse("\"*rentuximab vedot*\"");
        

        SearchResult sr=SearchResult.createBuilder().build();
        TopDocs hits = indexer.withSearcher(searcher->{
            try (TaxonomyReader taxon = new DirectoryTaxonomyReader(indexer.getTaxonWriter())) {
                return indexer.firstPassLuceneSearch(searcher,taxon,sr,null, q);
            }
        });

        assertEquals(4, hits.totalHits);

    }
    
   
    @Test
    public void confirmPhraseWildcardWithSpecialCharacterGetsResults() throws Exception{

        Document doc1 = new Document();
        doc1.add(new TextField("text", indexer.toExactMatchStringContinuous("brentuximab-tedotin"), NO));
        indexer.addDoc(doc1);
        Document doc2 = new Document();
        doc2.add(new TextField("text", indexer.toExactMatchStringContinuous("brentuximab&tedoton"), NO));
        indexer.addDoc(doc2);
        Document doc3 = new Document();
        doc3.add(new TextField("text", indexer.toExactMatchStringContinuous("brentuximab tevoton"), NO));
        indexer.addDoc(doc3);
        Document doc4 = new Document();
        doc4.add(new TextField("text", indexer.toExactMatchStringContinuous("brentuximad-tevoton"), NO));
        indexer.addDoc(doc4);
        Document doc5 = new Document();
        doc5.add(new TextField("text", indexer.toExactMatchStringContinuous("not actually brentuximab.tedotin"), NO));
        indexer.addDoc(doc5);
        
        Document doc6 = new Document();
        doc6.add(new TextField("text", indexer.toExactMatchStringContinuous("prentuximab-tedoton"), NO));
        indexer.addDoc(doc6);
            				
    	String processedQtext = indexer.preProcessQueryText("\"*rentuximab-tedot*\"");    				
    	Query q = indexer.getQueryParser().parse(processedQtext);     
             

        SearchResult sr=SearchResult.createBuilder().build();
        TopDocs hits = indexer.withSearcher(searcher->{
            try (TaxonomyReader taxon = new DirectoryTaxonomyReader(indexer.getTaxonWriter())) {
                return indexer.firstPassLuceneSearch(searcher,taxon,sr,null, q);
            }
        });   
        
        assertEquals(4, hits.totalHits);       
        
    }   
   
}
