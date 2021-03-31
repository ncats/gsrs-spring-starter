package gsrs.startertests.search.text;

import static org.apache.lucene.document.Field.Store.NO;

import java.io.File;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.GsrsSpringApplication;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import static org.junit.jupiter.api.Assertions.*;
import ix.core.search.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@GsrsJpaTest( classes = { GsrsSpringApplication.class, GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class})
public class TextIndexerQueryTest extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    private TextIndexerFactory tif;
    @Test
    public void confirmSimpleWildcardQueryIsNotComplex() throws Exception{
        TextIndexer indexer = tif.getDefaultInstance();
        Document doc = new Document();
        doc.add(new TextField("text", "brentuximab vedotin", NO));
                // now index
        indexer.addDoc(doc);

        Query q = indexer.parseQuery("brentuximab vedit*");
        SearchResult sr=SearchResult.createBuilder().build();
        TopDocs hits = indexer.withSearcher(searcher->{

            try (TaxonomyReader taxon = new DirectoryTaxonomyReader(indexer.getTaxonWriter())) {
                return indexer.firstPassLuceneSearch(searcher,taxon,sr,null, q);
            }

        });

        assertEquals(1, hits.totalHits);

    }

}
