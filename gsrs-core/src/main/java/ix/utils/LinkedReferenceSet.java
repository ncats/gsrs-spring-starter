package ix.utils;



import java.util.Optional;
import java.util.stream.Stream;

public class LinkedReferenceSet<K> implements ExecutionStack<K> {
	UniqueStack<StarterLiteralReference<K>> internalStack = new UniqueStack<StarterLiteralReference<K>>();
	
	
	public boolean contains(K k){
		return internalStack.contains(StarterLiteralReference.of(k));
	}
	
	
	@Override
	public void pushAndPopWith(K obj, Runnable r) {
		internalStack.pushAndPopWith(StarterLiteralReference.of(obj), r);
	}

	@Override
	public K getFirst() {
		return internalStack.getFirst().get();
	}
	
	@Override
	public Optional<K> getOptionalFirst() {
		Optional<StarterLiteralReference<K>> ret =internalStack.getOptionalFirst();
		if(ret.isPresent()){
			return Optional.of(ret.get().get());
		}
		return Optional.empty();
	}

	@Override
	public void setMaxDepth(Integer maxDepth) {
		internalStack.setMaxDepth(maxDepth);
	}

	public Stream<K> asStream(){
		return internalStack.asStream().map(l->l.get());
	}
	
	
}