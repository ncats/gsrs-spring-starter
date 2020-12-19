package gsrs.controller.hateoas;

import org.springframework.hateoas.Link;

import java.util.Collection;

public final class GsrsLinkUtil {
    private GsrsLinkUtil(){
        //can not instantiate
    }

    public static Link fieldLink(Collection<?> collection, String id, String fieldName, Link link){
        return new CollectionFieldLink(collection.size(), fieldName, link);
    }
    public static Link fieldLink(Object[] array, String id,  String fieldName, Link link){
        return new CollectionFieldLink(array.length, fieldName, link);
    }
    public static Link fieldLink(int[] array, String id,  String fieldName, Link link){
        return new CollectionFieldLink(array.length, fieldName, link);
    }

    public static Link fieldLink( String id, String fieldName, Link link){
        return new FieldLink(fieldName, link, id);
    }

    public static Link adapt( String id, Link link){
        return new FieldLink(null, link, id);
    }


}
