package ix.core.search;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import gsrs.cache.GsrsCache;
import gsrs.indexer.IndexValueMakerFactory;
import ix.core.search.bulk.UserSavedListService;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.core.search.text.Lucene4IndexServiceFactory;
import ix.core.search.text.ReflectingIndexValueMaker;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexer.IndexRecord;
import ix.core.search.text.TextIndexer.IndexedField;
import ix.core.search.text.TextIndexer.IndexedSuggestField;
import ix.core.search.text.TextIndexerConfig;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import lombok.Builder;
import lombok.Data;

public class TextIndexerIndexedRecordTest {

	@Entity
	@Data
	@Builder
	public static class TestEntity{
		@Id
		public Long id;
		public String field;
	}
	
    @TempDir
    static File file;
    
    
    
    
    private TextIndexer getNewTextIndexer() throws IOException {
    	Lucene4IndexServiceFactory fac = new Lucene4IndexServiceFactory();
		TextIndexerConfig conf = new TextIndexerConfig();
		conf.setEnabled(true);
		conf.setFieldsuggest(true);
		conf.setShouldLog(false);
		UserSavedListService userSavedListService = mock(UserSavedListService.class);
		Mockito.when(userSavedListService.getUserSearchResultLists(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(new ArrayList<String>());
				
		
		IndexValueMakerFactory singleIVMMaker = new IndexValueMakerFactory() {
			@Override
			public IndexValueMaker<Object> createIndexValueMakerFor(EntityWrapper<?> ew) {
				ReflectingIndexValueMaker rivm = new ReflectingIndexValueMaker();
				return rivm.and(new IndexValueMaker<Object>() {

					@Override
					public Class getIndexedEntityClass() {
						return ew.getClass();
					}
					@Override
					public void createIndexableValues(Object t, Consumer<IndexableValue> consumer) {
						consumer.accept(IndexableValue.simpleStringValue("foo", "bar"));
						consumer.accept(IndexableValue.simpleStringValue("Type Ahead", "TVALUE").suggestable());
					}
				});
			}
		};
		GsrsCache cache = mock(GsrsCache.class);
		TextIndexer ti= new TextIndexer(file, fac, fac.createForDir(file), conf, singleIVMMaker, cache, (ee)->false, userSavedListService);
		return ti;
    }
    
    private void assertFieldExistsWithValue(IndexRecord ir, String name, String value) {
    	IndexedField idf=ir.getFields().stream()
				   .filter(iff->iff.getFieldName().equals(name))
		           .findFirst()
		           .orElse(null);
		Assertions.assertNotNull(idf, "field '" + name + "' expected but not found");
		Assertions.assertEquals(value,idf.getFieldValue(), "expected indexed field value different");
    }
    private void assertSuggestFieldExistsWithValue(IndexRecord ir, String name, String value) {
    	IndexedSuggestField idf=ir.getSuggest().stream()
				   .filter(iff->iff.getSuggestName().equals(name))
		           .findFirst()
		           .orElse(null);
		Assertions.assertNotNull(idf, "field '" + name + "' expected but not found");
		Assertions.assertEquals(value,idf.getSuggestValue(), "expected indexed field value different");
    }
    
	@Test
	public void testIndexRecordGetsSavedAndCanBeRetrieved() throws NoSuchElementException, Exception {
		
		TextIndexer ti=getNewTextIndexer();
		
		TestEntity addTest = TestEntity.builder().id(1l).field("demo").build();
		EntityWrapper wrapped = EntityWrapper.of(addTest);
		ti.add(wrapped);
		IndexRecord ir = ti.getIndexRecord(wrapped.getKey());
		Assertions.assertNotNull(ir);
		Assertions.assertEquals(addTest.getId().toString(),ir.getId());
		assertFieldExistsWithValue(ir,"field","demo");
		assertFieldExistsWithValue(ir,"foo","bar");
		assertSuggestFieldExistsWithValue(ir, "Type Ahead", "TVALUE");
	}
	
	@Test
	public void testIndexRecordGetsSavedAndCanBeRetrievedAndRemoved() throws NoSuchElementException, Exception {
		TextIndexer ti=getNewTextIndexer();
		TestEntity addTest = TestEntity.builder().id(1l).field("demo").build();
		EntityWrapper wrapped = EntityWrapper.of(addTest);
		ti.add(wrapped);
		IndexRecord ir = ti.getIndexRecord(wrapped.getKey());
		Assertions.assertNotNull(ir);
		Assertions.assertEquals(addTest.getId().toString(),ir.getId());
		assertFieldExistsWithValue(ir,"field","demo");
		assertFieldExistsWithValue(ir,"foo","bar");
		assertSuggestFieldExistsWithValue(ir, "Type Ahead", "TVALUE");
		ti.remove(wrapped.getKey());
		IndexRecord ir2 = ti.getIndexRecord(wrapped.getKey());
		Assertions.assertNull(ir2);
	}
	
	@Test
	public void testIndexRecordGetsSavedAndCanBeSearched() throws NoSuchElementException, Exception {
		TextIndexer ti=getNewTextIndexer();
		TestEntity addTest = TestEntity.builder().id(1l).field("demo").build();
		EntityWrapper wrapped = EntityWrapper.of(addTest);
		ti.add(wrapped);

		//Simple searches
		SearchResult srMiss = ti.search(null, "foofoo:bar", 50);
		Assertions.assertTrue(srMiss.finished());
		Assertions.assertEquals(0,srMiss.getMatches().size());
		
		SearchResult sr = ti.search(null, "foo:bar", 50);
		Assertions.assertTrue(sr.finished());
		List<Key> res = new ArrayList<Key>();
		sr.copyKeysTo(res, 0, sr.size(), false);
		Assertions.assertEquals(1,res.size());
		Assertions.assertEquals(res.get(0).getIdNative(),addTest.id);
	}
	
}
