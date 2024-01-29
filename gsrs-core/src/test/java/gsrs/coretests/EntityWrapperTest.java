package gsrs.coretests;

import ix.core.models.Indexable;

import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.FieldMeta;
import org.junit.jupiter.api.Test;


import jakarta.persistence.Entity;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EntityWrapperTest{
	
	
	public static class TestIndexed{
		
		@Indexable(taxonomy=true)
		public String path;
		
		public TestIndexed(String path){
			this.path=path;
		}
	}
	@Test
	public void testPathSplitOnEntityWrapperSameAsManualSplit() throws Exception {

			TestIndexed testIndexed = new TestIndexed("this/is/a/path");
			EntityWrapper<TestIndexed> wrapped= EntityWrapper.of(testIndexed);
			Optional<FieldMeta> fieldMeta=wrapped.getFieldInfo()
				.stream()
				.filter(f->f.getName().equals("path"))
				.findFirst();
			assertTrue(fieldMeta.isPresent());
			FieldMeta fm = fieldMeta.get();
			String[] path=fm.getIndexable().splitPath(fm.getValue(testIndexed).get().toString());
			assertEquals(
					Arrays.asList(testIndexed.path.split("/")),
					Arrays.asList(path));

	}
	
	@Entity
	public static class Inception{
		@Indexable(indexed=true)
		public String lookHere="Look here";
		public Inception inc=this;
		
		public Child realChild = new Child();
		public Child nullChild = null;
		
	}
	
	@Entity
	public static class Child{
		
	}
	
	@Test
	public void testRecurseDoesNotGetIntoInfiniteLoop()  {
			Inception inc = new Inception();
			Set<String> expectedVisited = new HashSet<String>();
			expectedVisited.add("root");
			expectedVisited.add("root_realChild");
			Set<String> pathsVisited = new HashSet<String>();

			EntityWrapper.of(inc).traverse().execute((p, ew) -> {
				assertTrue(expectedVisited.contains(p.toPath()));
				pathsVisited.add(p.toPath());
			});

			assertEquals(expectedVisited, pathsVisited);
	}
	@Test
	public void recurseWithParent(){
		Inception inc = new Inception();
		Set<String> expectedVisited = new HashSet<String>();
		expectedVisited.add("root");
		expectedVisited.add("root_realChild");
		Set<String> pathsVisited = new HashSet<String>();
		List<Object> parentsSeen = new LinkedList<>();
		EntityWrapper.of(inc).traverse().execute((parent, path, ew) -> {
			assertTrue(expectedVisited.contains(path.toPath()));
			pathsVisited.add(path.toPath());
			parentsSeen.add(parent==null? null: parent.getValue());
		});

		assertEquals(expectedVisited, pathsVisited);
		assertEquals(Arrays.asList(null, inc), parentsSeen);
	}
}
