package ix.core.search.text;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;

import com.google.common.collect.Sets;

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
    public Set<String> getFieldNames(){
    	return list.stream().flatMap(ss->ss.getFieldNames().stream()).collect(Collectors.toSet());
    }
    
    
    @Override
    public IndexValueMaker<T> restrictedForm(Set<String> fields, boolean excludeExternal){
    	
    	
    	List<IndexValueMaker<T>> filteredList=list.stream()
    	.filter(ivm->fields.size()>0?ivm.getFieldNames().stream().anyMatch(fn->fields.contains(fn)):true)
    	.filter(ivm->excludeExternal?!ivm.isExternal():true)
    	.map(ivm->(IndexValueMaker<T>)ivm.restrictedForm(fields,excludeExternal))
    	.collect(Collectors.toList());
    	
    	//Todo for Lihui:  Remove after testing
    	Set<IndexValueMaker<T>> filteredOut = Sets.difference(new HashSet(list), new HashSet(filteredList)); 
    	System.out.println("In Combined index value maker: ");
    	filteredOut.forEach(ivm->System.out.println(ivm.getClass().getSimpleName()));
    	
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
