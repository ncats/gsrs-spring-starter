package ix.core.search.text;

import java.util.Collections;
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
    public Set<String> getTags() {
    	return list.stream().flatMap(ivm->ivm.getTags().stream()).collect(Collectors.toSet());
    }
    
    @Override
    public IndexValueMaker<T> restrictedForm(Set<String> tags, boolean include){
    	
    	if(tags.size() ==0) return this;    	
    	List<IndexValueMaker<T>> filteredList;    	
    	
    	if(include) {
    		System.out.println("in include combined");
    		filteredList=list.stream()
    			.filter(ivm->ivm.getTags().stream().anyMatch(tag->tags.contains(tag)))
    			.map(ivm->(IndexValueMaker<T>)ivm.restrictedForm(tags,include))
    		    	.collect(Collectors.toList());			
    	}else {
    		System.out.println("in exclude combined");
    		filteredList=list.stream()
        		.filter(ivm->!ivm.getTags().stream().anyMatch(tn->tags.contains(tn)))
        		.map(ivm->(IndexValueMaker<T>)ivm.restrictedForm(tags,include))
        		    .collect(Collectors.toList());    		
    		
    	}
    	
    	//Todo for Lihui:  Remove after testing
    	Set<IndexValueMaker<T>> filteredOut = Sets.difference(new HashSet(list), new HashSet(filteredList)); 
    	if(filteredOut.size()>0) {
    		System.out.println("IVMs are filtered out in combined index value maker: ");    	
    		filteredOut.forEach(ivm->System.out.println(ivm.getClass().getSimpleName()));
    	}
    	
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
