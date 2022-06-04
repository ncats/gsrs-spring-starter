package holding.area;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.holdingarea.model.ImportMetadata;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.GsrsSpringApplication;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;


import java.io.IOException;
import java.util.UUID;

@ActiveProfiles("test")
@GsrsJpaTest( classes = { GsrsSpringApplication.class, GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class})
public class ImportDataSearchTest extends AbstractGsrsJpaEntityJunit5Test {

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
    public void testIndex1() throws Exception {
        addSomeDocuments();
        Query q = indexer.getQueryParser().parse("\"Funky File\"");
        SearchResult sr=SearchResult.createBuilder().build();
        TopDocs hits = indexer.withSearcher(searcher->{
            try (TaxonomyReader taxon = new DirectoryTaxonomyReader(indexer.getTaxonWriter())) {
                return indexer.firstPassLuceneSearch(searcher,taxon,sr,null, q);
            }
        });
        Assertions.assertEquals(1, hits.scoreDocs.length);
        System.out.println("hits.scoreDocs.length: " + hits.scoreDocs.length);
    }

    @Test
    public void testIndex2() throws Exception {
        addSomeDocuments();
        Query q = indexer.getQueryParser().parse("root_ImportStatus:*accepted*"); //metadata_
        SearchResult sr=SearchResult.createBuilder().build();
        TopDocs hits = indexer.withSearcher(searcher->{
            try (TaxonomyReader taxon = new DirectoryTaxonomyReader(indexer.getTaxonWriter())) {
                return indexer.firstPassLuceneSearch(searcher,taxon,sr,null, q);
            }
        });
        Assertions.assertEquals(1, hits.scoreDocs.length);
        System.out.println("hits.scoreDocs.length: " + hits.scoreDocs.length);
    }

    private void addSomeDocuments() throws IOException {
        ImportMetadata importData = new ImportMetadata();
        UUID testRecordId = UUID.randomUUID();
        importData.setRecordId(testRecordId);
        importData.setSourceName("Funky File");
        importData.setImportStatus(ImportMetadata.RecordImportStatus.accepted);
        EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(importData);
        indexer.add(wrapper);

        ImportMetadata importMetadata = new ImportMetadata();
        UUID recordId = UUID.randomUUID();
        importMetadata.setImportStatus(ImportMetadata.RecordImportStatus.imported);
        importMetadata.setEntityClass("Substance");
        importMetadata.setSourceName("Normal File");
        importMetadata.setRecordId(recordId);
        EntityUtils.EntityWrapper wrapper2 = EntityUtils.EntityWrapper.of(importMetadata);
        indexer.add(wrapper2);

    }
}
