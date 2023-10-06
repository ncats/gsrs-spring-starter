package ix.core.search.text;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An IndexValueMaker is intended to be used to produce indexable fields, 
 * facets, suggest fields, etc from a given entity.
 * 
 * Specifically, the interface provides a mechanism to start from an Object
 * t, and deliver IndexableValues via a supplied consumer. 
 * 
 * 
 * @author peryeata
 * @author katzelda
 *
 * @param <T>
 */
public interface IndexValueMaker<T> {
	/**
	 * Get the Class of type T (and subclasses) we can index.
	 * @return the Class of T.
	 *
	 * @since 3.0
	 */
	Class<T> getIndexedEntityClass();
	/**
	 * Creates IndexableValues out of the given Entity T, and returns them
	 * to an awaiting Consumer. The IndexableValues may then be used to
	 * populate a full text index, type-ahead suggest, sorting function 
	 * and/or facets.
	 * 
	 * @param t
	 * @param consumer
	 */
	void createIndexableValues(T t, Consumer<IndexableValue> consumer);
	
	
	default Set<String> getFieldNames(){
		return new HashSet<>();
	}
	
	default IndexValueMaker<T> restrictedForm(Set<String> fields){
		return this;
	}
	
	/**
	 * Combine 2 IndexValueMakers together, so that
	 * each is called sequentially. This accumulation
	 * allows for a given {@link IndexValueMaker} to fail
	 * with an exception, and will still continue on.
	 * 
	 * @param other
	 * @return
	 */
	default IndexValueMaker<T> and(IndexValueMaker<T> other){
		return new CombinedIndexValueMaker(this.getIndexedEntityClass(), Arrays.asList(this, other));
	}
}
