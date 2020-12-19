package gsrs.controller.hateoas;

import gov.nih.ncats.common.util.CachedSupplier;
import ix.core.FieldResourceReference;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.lang.reflect.Array;
import java.util.Collection;


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
                GsrsUnwrappedEntityModel<?> childModel = (GsrsUnwrappedEntityModel<?>)child;
                handleSingleObject(childModel, childModel.getObj());
            }
            return model;
        }
        return handleSingleObject(model, obj);
    }

    private GsrsUnwrappedEntityModel<?> handleSingleObject(GsrsUnwrappedEntityModel<?> model, Object obj) {
        EntityUtils.EntityInfo<?> info = EntityUtils.EntityWrapper.of(obj).getEntityInfo();
        CachedSupplier<String> idString = info.getIdString(obj);
        String id = idString.get();
        //always add self
        model.add(computeSelfLink(model, id));

        for(EntityUtils.MethodMeta action : info.getApiActions()){
            Object resource =action.getValue(obj).get();
            if(resource !=null && resource instanceof FieldResourceReference){
                String field = ((FieldResourceReference)resource).computedResourceLink();

                model.add(
                        GsrsLinkUtil.fieldLink(id, action.getJsonFieldName(),entityLinks.linkFor(obj.getClass())
                        .slash("("+id +")") // this is a hack to fake the url we fix it downstream in the GsrsLinkUtil class
                        .slash(field)
                        .withRel(action.getJsonFieldName())));

//                String field = ((FieldResourceReference)resource).computedResourceLink();
//                model.add(GsrsLinkUtil.fieldLink(field, linkTo(methodOn(model.getController()).getFieldById(id, Collections.emptyMap(), null))
//                                                .withRel(action.getJsonFieldName()).expand(id)));

            }
        }
        if(model.isCompact()) {
            info.getCollapsedFields().forEach(f -> {
                String compactFieldName = f.getCompactViewFieldName();
                String field = f.getName();


                System.out.println(field);
                if (f.isCollection()) {
                    model.add( computeFieldLink(((Collection) f.getValue(obj).get()).size(),
                            obj, id, field, compactFieldName));
                } else if (f.isArray()) {
                    model.add(
                            computeFieldLink(Array.getLength(f.getValue(obj).get()),
                                    obj, id, field, compactFieldName));
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
        return entityLinks.linkFor(model.getObj().getClass());
    }
    private Link  computeFieldLink(int collectionSize, Object entity, String id, String fieldPath, String rel){
       return new CollectionLink(collectionSize, GsrsLinkUtil.adapt(id, entityLinks.linkFor(entity.getClass())
                .slash("("+id +")") // this is a hack to fake the url we fix it downstream in the GsrsLinkUtil class
                .slash(fieldPath)
                .withRel(rel)));

    }
    private Link computeSelfLink(GsrsUnwrappedEntityModel<?> model, String id) {


        Link l= GsrsLinkUtil.adapt(id, entityLinks.linkFor(model.getObj().getClass())
                .slash("("+id +")") // this is a hack to fake the url we fix it downstream in the GsrsLinkUtil class

                .withRel("_self"));
        String query=l.toUri().getRawQuery();
        if(query==null){
            return l.withHref(l.getHref() +"?view=full");
        }
        return l.withHref(l.getHref() +"&view=full");


    }

}
