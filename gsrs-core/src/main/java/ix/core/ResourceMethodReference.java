package ix.core;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.function.Supplier;

@Slf4j
@JsonSerialize(using = ResourceMethodReference.ResourceReferenceSerializer.class)
public class ResourceMethodReference<T> extends ResourceReference<T>{

	public static <T> ResourceMethodReference<T> forMethod(String uri, Supplier<T> methodReference){
		return new ResourceMethodReference(uri, methodReference);
	}
	public ResourceMethodReference(String uri, Supplier<T> sup){
		super(uri, sup);
	}

    /**
     * Invoke the method.
	 * @return
     */
	public T invoke(){
		return super.invoke();
	}

		
	
}
