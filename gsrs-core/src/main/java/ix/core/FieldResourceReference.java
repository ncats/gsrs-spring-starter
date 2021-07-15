package ix.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.function.Supplier;
@Slf4j
public class FieldResourceReference<T> extends ResourceReference<T> {


    public FieldResourceReference(String uri, Supplier<T> sup, String...params) {
        super(uri, sup,params);
    }
    public static <T> FieldResourceReference<T> forField(String field, Supplier<T> supplier){
        return forField(field, false, supplier);
    }
    public static <T> FieldResourceReference<T> forRawField(String field, Supplier<T> supplier){
        return forField(field, true, supplier);
    }
    public static <T> FieldResourceReference<T> forField(String field, boolean isRaw, Supplier<T> supplier, String...params){
        Objects.requireNonNull(field);
        Objects.requireNonNull(supplier);
        return new FieldResourceReference(isRaw? "$"+field : field, supplier, params);
    }
    public static <T> FieldResourceReference<T> forSelf( Supplier<T> supplier, String...params){
        return forField("", false, supplier, params);
    }

    //
    public static FieldResourceReference<JsonNode> forFieldAsJson(String field, Supplier<String> supplier){
        return forFieldAsJson(field, false, supplier);
    }
    public static  FieldResourceReference<JsonNode>  forRawFieldAsJson(String field, Supplier<String> supplier){
        return forFieldAsJson(field, true, supplier);
    }
    public static FieldResourceReference<JsonNode> forFieldAsJson(String field, String stringValue){
        return forFieldAsJson(field, false, ()->stringValue);
    }
    public static  FieldResourceReference<JsonNode>  forRawFieldAsJson(String field, String stringValue){
        return forFieldAsJson(field, true, ()->stringValue);
    }
    public static FieldResourceReference<JsonNode>  forFieldAsJson(String field, boolean isRaw, Supplier<String> supplier){
        Objects.requireNonNull(field);
        Objects.requireNonNull(supplier);
        return new FieldResourceReference<JsonNode> (isRaw? "$"+field : field, ()->{
            ObjectMapper om = new ObjectMapper();
            try {
                return om.readTree(supplier.get());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            return null;
        });
    }

}
