package ix.core.util.pojopointer.extensions;

import ix.core.util.pojopointer.LambdaArgumentParser;
import ix.core.util.pojopointer.LambdaPath;

import java.util.Optional;
import java.util.function.BiFunction;

public interface RegisteredFunction<T extends LambdaPath, U, V>{

	public Class<T> getFunctionClass();
	public LambdaArgumentParser<T> getFunctionURIParser();
	public BiFunction<T, U, Optional<V>> getOperation();
	
	
	default String getFunctionName(){
		return getFunctionURIParser().getKey();
	}
	
}
