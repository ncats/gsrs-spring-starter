package ix.utils;



import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;


/**
 * Obviously not thread-safe. Don't try to use the same PathStack in two
 * different threads!
 * @author peryeata
 *
 */
public class PathStack implements ExecutionStack<String>{
	LinkedList<String> realStack = new LinkedList<String>();
	
	private Integer maxDepth;

	@Override
	public void setMaxDepth(Integer maxDepth) {
		if(maxDepth !=null && maxDepth.intValue() <1){
			throw new IllegalArgumentException("max depth can not be negative");
		}
		this.maxDepth = maxDepth;
	}

	/* (non-Javadoc)
	 * @see ix.core.search.text.ExecutionStack#pushAndPopWith(K, java.lang.Runnable)
	 */
	@Override
	public void pushAndPopWith(String obj, Runnable r){
		if(!isAcceptableDepth()){
			return;
		}
		realStack.push(obj);
		try{
			r.run();
		}finally{
			realStack.pop();
		}
	}
	
	private boolean isAcceptableDepth(){
		return maxDepth ==null || maxDepth.intValue() > getDepth();
	}

	
	public void pushAndPopWith(List<String> obj, Runnable r){
		if(!isAcceptableDepth()){
			return;
		}
		realStack.addAll(obj);
		try{
			r.run();
		}finally{
			IntStream.range(0, obj.size()).forEach(i->{
				realStack.pop();
			});
		}
	}
	
	
	
	/* (non-Javadoc)
	 * @see ix.core.search.text.ExecutionStack#getFirst()
	 */
	@Override
	public String getFirst() {
		return realStack.getFirst();
	}
	
	@Override
	public Optional<String> getOptionalFirst() {
		if(realStack.isEmpty()){
			return Optional.empty();
		}
		return Optional.ofNullable(realStack.getFirst());
	}

	//Pretty lazy, really ... but don't optimize it
	public String toPath() {
		return  toPath(p->!isNumeric( p));

	}
	private static boolean isNumeric(CharSequence cs) {
		if (cs == null || cs.length() == 0) {
			return false;
		}
		int sz = cs.length();
		for (int i = 0; i < sz; i++) {
			if (Character.isDigit(cs.charAt(i)) == false) {
				return false;
			}
		}
		return true;
	}

	public String toPath(Predicate<String> predicate) {
		StringBuilder sb = new StringBuilder(256);
		// TP: Maybe do this?
//		sb.append(TextIndexer.ROOT); //TODO: abstract this away somehow?
		//TODO katzelda October 2020 : hardcode TextIndexer.ROOT for now
		sb.append("root");

		for (Iterator<String> it = realStack.descendingIterator(); it.hasNext();) {
			String p =  it.next();
			if (predicate.test(p)) {
				sb.append('_').append(p);

			}
		}
		return sb.toString();
	}

	public int getDepth(){
		return realStack.size();
	}
}