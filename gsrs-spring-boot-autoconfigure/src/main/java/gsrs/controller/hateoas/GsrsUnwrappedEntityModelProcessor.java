package gsrs.controller.hateoas;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.model.GsrsApiAction;
import gsrs.model.Sizeable;
import ix.core.FieldResourceReference;
import ix.core.ObjectResourceReference;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Optional;


/**
 * A GSRS Controller Aware {@link RepresentationModelProcessor}
 * that will add HATEOAS like links in a GSRS API backwards compatible way.
 * This Processor will add a "_self" JSON attribute to the Model object as well as add other
 * attributes if needed such as:
 * <ul>
 * <li>If the Request's view is set to "compact". then this will use the {@link ix.core.util.EntityUtils.EntityInfo}
 * of the model object to find any collapsed fields via {@link ix.core.util.EntityUtils.EntityInfo#getCollapsedFields()}
 * method and create links to the API with an href and count attribute.</li>
 *
 * <li>If the Entity has any methods annotated with {@link gsrs.model.GsrsApiAction} those will
 * be turned into links</li>
 * </ul>
 *
 * All these links will be further transformed to use the standard GSRS api/v1 prefix
 * and fix any of the links that use the $controller($id) paths as well
 * as the generic HATEOAS link builders won't include that part since it's not in the requestMapping annotations.
 */
public class GsrsUnwrappedEntityModelProcessor implements RepresentationModelProcessor<GsrsUnwrappedEntityModel<?>> {
   @Autowired
   private EntityLinks entityLinks;

    @Override
    public GsrsUnwrappedEntityModel<?> process(GsrsUnwrappedEntityModel<?> model) {
        Object obj = model.getObj();
        if(obj instanceof Collection){
            for(Object child : (Collection)obj){
                if(child instanceof GsrsUnwrappedEntityModel) {
                    GsrsUnwrappedEntityModel<?> childModel = (GsrsUnwrappedEntityModel<?>) child;
                    handleSingleObject(childModel, childModel.getObj());
                }
            }
            return model;
        }
        return handleSingleObject(model, obj);
    }

    /**
     * The @ExposeResourceFor annotation in spring HATAEOS only allows mapping to a single class.
     * This causes problems when the entity has subclasses (like GSRS Substance) the subclasses won't map
     * and throw exceptions.  So this iteratively calls super classes until it finds a working entity link
     * or gets all the way to Object.
     * @param c the class to get the controller link for
     * @return the {@link LinkBuilder} for that controller.
     * @throws IllegalArgumentException if the given class or any parent class does not have a controller.
     */
    private LinkBuilder getEntityLinkForClassOrParentClass(Class<?> c){
        do {
            try {
                return entityLinks.linkFor(c);
            }catch(Exception e){}
            c = c.getSuperclass();
        }while(c !=null);
        throw new IllegalArgumentException("invalid entity link class " + c);
    }

    private GsrsUnwrappedEntityModel<?> handleSingleObject(GsrsUnwrappedEntityModel<?> model, Object obj) {
        EntityUtils.EntityInfo<?> info = EntityUtils.EntityWrapper.of(obj).getEntityInfo();
        CachedSupplier<String> idString = info.getIdString(obj);
        String id = idString.get();
        //always add self
        model.add(computeSelfLink(model, id));

        for(EntityUtils.MethodMeta action : info.getApiActions()){
            Object resource =action.getValue(obj).get();
            if(resource !=null) {
                //GSRS 2.x RestUrlLink object has 2 fields : rul and type (for GET, DELETE etc)
                GsrsApiAction gsrsApiAction = action.getAnnotation(GsrsApiAction.class);
                String type = gsrsApiAction ==null? "GET": gsrsApiAction.type().name();

                if (resource instanceof FieldResourceReference) {
                    String field = ((FieldResourceReference) resource).computedResourceLink();

                    model.add(
                            GsrsLinkUtil.fieldLink(id, action.getJsonFieldName(),getEntityLinkForClassOrParentClass(obj.getClass())
                                    .slash("(" + id + ")") // this is a hack to fake the url we fix it downstream in the GsrsLinkUtil class
                                    .slash(field)
                                    .withRel(action.getJsonFieldName())),
                            type);

//                String field = ((FieldResourceReference)resource).computedResourceLink();
//                model.add(GsrsLinkUtil.fieldLink(field, linkTo(methodOn(model.getController()).getFieldById(id, Collections.emptyMap(), null))
//                                                .withRel(action.getJsonFieldName()).expand(id)));

                }else if(resource instanceof ObjectResourceReference){
                    ObjectResourceReference objResource = (ObjectResourceReference)resource;
                    LinkBuilder linkBuilder = getEntityLinkForClassOrParentClass(objResource.getEntityClass())
                            .slash("(" + objResource.getId() + ")");
                    if(objResource.getFieldPath()!=null){
                        linkBuilder = linkBuilder.slash(objResource.getFieldPath());
                    }
                    model.add(GsrsLinkUtil.fieldLink(id, action.getJsonFieldName(), linkBuilder
                                    .withRel(action.getJsonFieldName())),
                            type);
                }
            }
        }
        if(model.isCompact()) {
            info.getCollapsedFields().forEach(f -> {
                String compactFieldName = f.getCompactViewFieldName();
                String field = f.getName();


                System.out.println(field);
                Optional<Object> value = f.getValue(obj);
                if(value.isPresent()) {
                    if (f.isCollection()) {
                        model.add(computeFieldLink(((Collection) value.get()).size(),
                                obj, id, field, compactFieldName));
                    } else if (f.isArray()) {
                        model.add(
                                computeFieldLink(Array.getLength(value.get()),
                                        obj, id, field, compactFieldName));
                    } else if (Sizeable.class.isAssignableFrom(f.getType())) {
                        model.add(computeFieldLink(((Sizeable) value.get()).getSize(),
                                obj, id, field, compactFieldName));
                    }
                }
//            else{
//                model.add(
//                        linkTo(methodOn(model.getController()).getFieldById(id, Collections.emptyMap(), null)).withRel(compactFieldName).getTemplate());
//
//            }
            });
        }
        return model;
    }

    private LinkBuilder getControllerLinkFor(GsrsUnwrappedEntityModel<?> model){
        return getEntityLinkForClassOrParentClass(model.getObj().getClass());
    }
    private Link  computeFieldLink(int collectionSize, Object entity, String id, String fieldPath, String rel){
       return new CollectionLink(collectionSize, GsrsLinkUtil.adapt(id, getEntityLinkForClassOrParentClass(entity.getClass())
                .slash("("+id +")") // this is a hack to fake the url we fix it downstream in the GsrsLinkUtil class
                .slash(fieldPath)
                .withRel(rel)));

    }
    private Link computeSelfLink(GsrsUnwrappedEntityModel<?> model, String id) {


        Link l= GsrsLinkUtil.adapt(id, getEntityLinkForClassOrParentClass(model.getObj().getClass())
                .slash("("+id +")") // this is a hack to fake the url we fix it downstream in the GsrsLinkUtil class

                .withRel("_self"));
        String query=l.toUri().getRawQuery();
        if(query==null){
            return l.withHref(l.getHref() +"?view=full");
        }
        return l.withHref(l.getHref() +"&view=full");


    }

}
