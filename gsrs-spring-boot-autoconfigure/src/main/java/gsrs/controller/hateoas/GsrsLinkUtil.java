package gsrs.controller.hateoas;

import ix.core.EntityMapperOptions;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkBuilder;

import java.util.Collection;
import java.util.Optional;

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
    public static Link adapt( Link link){
        return new FieldLink(null, link, null);
    }

    /**
     * The @ExposeResourceFor annotation in spring HATAEOS only allows mapping to a single class.
     * This causes problems when the entity has subclasses (like GSRS Substance) the subclasses won't map
     * and throw exceptions.  So this iteratively calls super classes until it finds a working entity link
     * or gets all the way to Object.
     * @param c the class to get the controller link for
     * @param entityLinks the EntityLinks object
     * @return the {@link LinkBuilder} for that controller wrapped in an Optional; or empty if there is no
     * entity associated with a controller.
     * @throws IllegalArgumentException if the given class or any parent class does not have a controller.
     */
    public static Optional<LinkBuilder> getEntityLinkForClassOrParentClass(Class<?> c, EntityLinks entityLinks){
        do {
            try {
                return Optional.of(entityLinks.linkFor(c));
            }catch(Exception e){}
            c = c.getSuperclass();
        }while(c !=null);
        return Optional.empty();
    }

    public static Link computeSelfLinkFor(EntityLinks entityLinks, Class<?> EntityClass, String id) {
        Optional<LinkBuilder> linkBuilder = GsrsLinkUtil.getEntityLinkForClassOrParentClass(EntityClass, entityLinks);
        if(!linkBuilder.isPresent()){
            return null;
        }
        String selfRel = "_self";
        EntityMapperOptions options = EntityClass.getAnnotation(EntityMapperOptions.class);
        if(options !=null){
            selfRel = options.getSelfRel();
        }

        Link l= GsrsLinkUtil.adapt(id, linkBuilder.get()
                .slash("("+ id +")") // this is a hack to fake the url we fix it downstream in the GsrsLinkUtil class

                .withRel(selfRel));
        String query=l.toUri().getRawQuery();
        if(query==null){
            return l.withHref(l.getHref() +"?view=full");
        }
        return l.withHref(l.getHref() +"&view=full");
    }
}
