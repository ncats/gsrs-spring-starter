package ix.core.search.text;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import gsrs.cache.GsrsCache;
import gsrs.indexer.IndexValueMakerFactory;
import ix.core.search.TextIndexerIndexedRecordTest;
import ix.core.search.TextIndexerIndexedRecordTest.TestEntity;
import ix.core.search.bulk.UserSavedListService;
import ix.core.search.text.TextIndexer.IndexRecord;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.utils.Util;
import lombok.Builder;
import lombok.Data;

public class RestrictedFormIVMTest {
	
	private IndexValueMakerFactory restrictedFromIVMMakerFactory;
	
	@Entity
	@Data
	@Builder
	public static class TestEntity{
		@Id
		public Long id;
		public String name;
		public String code;
	}
	
	@TempDir
	static File file;
	
	class TestCodeIndexValueMaker implements IndexValueMaker<TestEntity>{
		
		@Override
		public Class getIndexedEntityClass() {
			return TestEntity.class;
		}
		
		@Override
		public Set<String> getFieldNames(){
			return Util.toSet("CAS");
		}
			
		@Override
		public Set<String> getTags(){
			return Util.toSet("CAS_code");
		}

		@Override
		public void createIndexableValues(TestEntity t, Consumer<IndexableValue> consumer) {
			consumer.accept(IndexableValue.simpleStringValue("CAS", "161622-14-6"));			
		}		
	}
	
	class TestNameIndexValueMaker implements IndexValueMaker<TestEntity>{
		
		@Override
		public Class getIndexedEntityClass() {
			return TestEntity.class;
		}		
		
		@Override
		public Set<String> getFieldNames(){
			return Util.toSet("std name");
		}
			
		@Override
		public Set<String> getTags(){
			return Util.toSet("std_name");
		}
		
		@Override
		public void createIndexableValues(TestEntity t, Consumer<IndexableValue> consumer) {
			consumer.accept(IndexableValue.simpleStringValue("std name", "test_name"));						
		}
	}
	
	
	private TextIndexer getNewTextIndexer() throws IOException {
    	Lucene4IndexServiceFactory fac = new Lucene4IndexServiceFactory();
		TextIndexerConfig conf = new TextIndexerConfig();
		conf.setEnabled(true);
		conf.setFieldsuggest(true);
		conf.setShouldLog(false);
		UserSavedListService userSavedListService = mock(UserSavedListService.class);
		Mockito.when(userSavedListService.getUserSearchResultLists(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(new ArrayList<String>());
				
		
		IndexValueMakerFactory restrictedIVMMaker = new IndexValueMakerFactory() {
			
			@Override
			public IndexValueMaker<Object> createIndexValueMakerFor(EntityWrapper<?> ew) {				
				
				IndexValueMaker<TestEntity> nameIndexer = new TestNameIndexValueMaker();
				IndexValueMaker<TestEntity> codeIndexer = new TestCodeIndexValueMaker();				
				
				List<IndexValueMaker<TestEntity>> list = new ArrayList<IndexValueMaker<TestEntity>>();
				list.add(nameIndexer);
				list.add(codeIndexer);					
				Class<?> cls = ew.getEntityClass();
				
				return new CombinedIndexValueMaker(cls, list);
			}
		};
		
		this.restrictedFromIVMMakerFactory = restrictedIVMMaker;
		GsrsCache cache = mock(GsrsCache.class);
		TextIndexer ti= new TextIndexer(file, fac, fac.createForDir(file), conf, restrictedIVMMaker, cache, (ee)->false, userSavedListService);
		return ti;
    }
	
	
	@Test
	public void testStrictedIVMApplied() throws NoSuchElementException, Exception {
		TextIndexer ti=getNewTextIndexer();
		TestEntity addTest = TestEntity.builder().id(1l).name("demo1").code("161622-14-2").build();
		EntityWrapper wrapped = EntityWrapper.of(addTest);
		
		
		IndexValueMaker<Object> includeCodeIVMmaker = restrictedFromIVMMakerFactory.createIndexValueMakerFor(wrapped)
				.restrictedForm(Util.toSet("CAS_code"), true);		
//		System.out.println("include code "+includeIVMmaker.getFieldNames());
		Assertions.assertTrue(includeCodeIVMmaker.getFieldNames().contains("CAS"));
		Assertions.assertEquals(includeCodeIVMmaker.getFieldNames().size(),1);
		
		
		IndexValueMaker<Object> excludeCodeIVMmaker = restrictedFromIVMMakerFactory.createIndexValueMakerFor(wrapped)
				.restrictedForm(Util.toSet("CAS_code"), false);
//		System.out.println("exclude code "+includeIVMmaker.getFieldNames());
		Assertions.assertFalse(excludeCodeIVMmaker.getFieldNames().contains("CAS"));
		Assertions.assertEquals(excludeCodeIVMmaker.getFieldNames().size(),1);
		
		IndexValueMaker<Object> includeNameIVMmaker = restrictedFromIVMMakerFactory.createIndexValueMakerFor(wrapped)
				.restrictedForm(Util.toSet("std_name"), true);		
//		System.out.println("include code "+includeIVMmaker.getFieldNames());
		Assertions.assertTrue(includeNameIVMmaker.getFieldNames().contains("std name"));
		Assertions.assertEquals(includeNameIVMmaker.getFieldNames().size(),1);
		
		
		IndexValueMaker<Object> excludeNameIVMmaker = restrictedFromIVMMakerFactory.createIndexValueMakerFor(wrapped)
				.restrictedForm(Util.toSet("std_name"), false);
//		System.out.println("exclude code "+includeIVMmaker.getFieldNames());
		Assertions.assertFalse(excludeNameIVMmaker.getFieldNames().contains("std name"));
		Assertions.assertEquals(excludeNameIVMmaker.getFieldNames().size(),1);		
		
		
		RestrictedIVMSpecification specs = new RestrictedIVMSpecification(true, Util.toSet("CAS_code"));
		ti.add(wrapped, true, specs);
		IndexRecord ir = ti.getIndexRecord(wrapped.getKey());
		Assertions.assertNotNull(ir);
		Assertions.assertEquals(addTest.getId().toString(),ir.getId());	
		ti.remove(wrapped.getKey());
		IndexRecord ir2 = ti.getIndexRecord(wrapped.getKey());
		Assertions.assertNull(ir2);
	}	

}
