package ix.core.search.text;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;

public class CombinedIndexValueMaker<T> implements IndexValueMaker<T> {
    private final List<IndexValueMaker<? super T>> list;
    private final Class<T> clazz;
    public CombinedIndexValueMaker(Class<T> clazz, List<IndexValueMaker<? super T>> list) {
        this.list = list;
        this.clazz = clazz;
    }

    @Override
    public Class<T> getIndexedEntityClass() {
        return clazz;
    }
    
    @Override
    public IndexValueMaker<T> restrictedForm(Set<String> fields){
    	List<IndexValueMaker<T>> filteredList=list.stream()
    	.filter(ivm->ivm.getFieldNames().stream().anyMatch(fn->fields.contains(fn)))
    	.map(ivm->(IndexValueMaker<T>)ivm.restrictedForm(fields))
    	.collect(Collectors.toList());
    	
    	return new CombinedIndexValueMaker(clazz,filteredList);
    }

    @Override
    public void createIndexableValues(T t, Consumer<IndexableValue> c) {
        list.forEach(i -> {
            try {
                i.createIndexableValues(t, c);
            } catch (Throwable e) {
                e.printStackTrace();
//		        Logger.error("Trouble creating index for:" + EntityWrapper.of(t).getKey(), e);
            }
        });

    }
}
