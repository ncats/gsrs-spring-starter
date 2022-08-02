package gsrs.dataexchange.area;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.holdingArea.model.ImportMetadata;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
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
import gsrs.GsrsSpringApplication;

import java.io.IOException;
import java.util.UUID;

@ActiveProfiles("test")
@GsrsJpaTest( classes = { GsrsSpringApplication.class, GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class})
class ImportDataSearchTest extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    private TextIndexerFactory tif;

    private TextIndexer indexer;

    private String objectName="gsrs.holdingArea.model.ImportMetadata";
    @BeforeEach
    public void setup() throws Exception {
        indexer = tif.getDefaultInstance();
        addSomeDocuments();
    }

    @AfterEach
    public void teardown() throws Exception {
        indexer.remove(new MatchAllDocsQuery());
    }


    @Test
    public void testIndexName() throws Exception {
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
    public void testIndexImportStatus() throws Exception {
        Query q = indexer.getQueryParser().parse("root_importStatus:\"^accepted$\""); //metadata_
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
    public void testIndexImportStatus2() throws Exception {
        Query q = indexer.getQueryParser().parse("root_processStatus:\"^loaded$\"");
        SearchResult sr=SearchResult.createBuilder().build();
        TopDocs hits = indexer.withSearcher(searcher->{
            try (TaxonomyReader taxon = new DirectoryTaxonomyReader(indexer.getTaxonWriter())) {
                return indexer.firstPassLuceneSearch(searcher,taxon,sr,null, q);
            }
        });
        Assertions.assertEquals(2, hits.scoreDocs.length);
        System.out.println("hits.scoreDocs.length: " + hits.scoreDocs.length);
    }

    @Test
    public void testIndexSearchEntityClass() throws Exception {
        Query q = indexer.getQueryParser().parse("root_entityClassName:\"^" + objectName + "$\"");
        SearchResult sr=SearchResult.createBuilder().build();
        TopDocs hits = indexer.withSearcher(searcher->{
            try (TaxonomyReader taxon = new DirectoryTaxonomyReader(indexer.getTaxonWriter())) {
                return indexer.firstPassLuceneSearch(searcher,taxon,sr,null, q);
            }
        });
        Assertions.assertEquals(4, hits.scoreDocs.length);
        System.out.println("hits.scoreDocs.length: " + hits.scoreDocs.length);
    }

    @Test
    public void testIndexSearchValidationStatus() throws Exception {
        Query q = indexer.getQueryParser().parse("root_validationStatus:\"^valid$\"");
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
        ImportMetadata importMetadata = new ImportMetadata();
        UUID testRecordId = UUID.randomUUID();
        importMetadata.setRecordId(testRecordId);
        UUID instanceId = UUID.randomUUID();
        importMetadata.setInstanceId(instanceId);
        importMetadata.setEntityClassName(objectName);
        importMetadata.setSourceName("Funky File");
        importMetadata.setImportStatus(ImportMetadata.RecordImportStatus.accepted);
        EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(importMetadata);
        indexer.add(wrapper);

        ImportMetadata importMetadata2 = new ImportMetadata();
        UUID recordId = UUID.randomUUID();
        importMetadata2.setImportStatus(ImportMetadata.RecordImportStatus.imported);
        importMetadata2.setEntityClassName(objectName);
        importMetadata2.setSourceName("Normal File");
        importMetadata2.setRecordId(recordId);
        instanceId = UUID.randomUUID();
        importMetadata2.setInstanceId(instanceId);
        EntityUtils.EntityWrapper wrapper2 = EntityUtils.EntityWrapper.of(importMetadata2);
        indexer.add(wrapper2);

        ImportMetadata importMetadata3 = new ImportMetadata();
        UUID recordId3 = UUID.randomUUID();
        importMetadata3.setImportStatus(ImportMetadata.RecordImportStatus.staged);
        importMetadata3.setEntityClassName(objectName);
        importMetadata3.setSourceName("Experimental File");
        importMetadata3.setProcessStatus(ImportMetadata.RecordProcessStatus.loaded);
        importMetadata3.setRecordId(recordId3);
        instanceId = UUID.randomUUID();
        importMetadata3.setInstanceId(instanceId);
        EntityUtils.EntityWrapper wrapper3 = EntityUtils.EntityWrapper.of(importMetadata3);
        indexer.add(wrapper3);

        ImportMetadata importMetadata4 = new ImportMetadata();
        UUID recordId4 = UUID.randomUUID();
        importMetadata4.setImportStatus(ImportMetadata.RecordImportStatus.staged);
        importMetadata4.setEntityClassName(objectName);
        importMetadata4.setSourceName("Additional File");
        importMetadata4.setProcessStatus(ImportMetadata.RecordProcessStatus.loaded);
        importMetadata4.setVersion(7);
        importMetadata4.setValidationStatus(ImportMetadata.RecordValidationStatus.valid);
        importMetadata4.setRecordId(recordId4);
        instanceId = UUID.randomUUID();
        importMetadata4.setInstanceId(instanceId);
        EntityUtils.EntityWrapper wrapper4 = EntityUtils.EntityWrapper.of(importMetadata4);
        indexer.add(wrapper4);
    }
}
