package ix.core.search.text;

import static org.apache.lucene.document.Field.Store.NO;

import java.io.File;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import ix.core.search.SearchResult;

public class TextIndexerQueryTest {
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();
    
//    @Test
//    public void confirmSimpleWildcardQueryIsNotComplex() throws Exception{
//        TextIndexerFactory tif = new TextIndexerFactory();
//        folder.create();
//       
//        TextIndexer tindexer =tif.getInstance(folder.getRoot());
//        
//        
//        Document doc = new Document();
//        doc.add(new TextField("text", "brentuximab vedotin", NO));
//                // now index
//        tindexer.addDoc(doc);
//        
//        Query q = tindexer.parseQuery("brentuximab vedit*");
//        
//        SearchResult sr=SearchResult.createBuilder().build();
//        tindexer.withSearcher(searcher->{
//
//            final TopDocs hits;
//            try (TaxonomyReader taxon = new DirectoryTaxonomyReader(tindexer.getTaxonWriter())) {
//                hits=tindexer.firstPassLuceneSearch(searcher,taxon,sr,null, q);
//            }
//            System.out.println(hits.totalHits);
//            
//            return true;
//        });
//        
//        
//    }
}
