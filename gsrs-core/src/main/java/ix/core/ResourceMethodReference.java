package ix.core;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ix.core.util.EntityUtils.EntityWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;
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
