package ix.utils.pojopatch;

import java.util.List;
import java.util.Stack;

public interface PojoPatch<T>{
	//public Stack apply(T old) throws Exception;
	public Stack<?> apply(T old, ChangeEventListener... changeListener) throws Exception;
	
	
	public List<Change> getChanges();
	
	
}