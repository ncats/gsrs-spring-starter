package gsrs.controller.hateoas;

import gov.nih.ncats.common.util.CachedSupplier;
import ix.core.FieldResourceReference;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * A GSRS Controller Aware {@link RepresentationModelProcessor}
 * that will add HATEOAS like links in a GSRS API backwards compatible way.
 * This Processor will add a "_self" JSON attribute to the Model object as well as add other
 * attributes if needed such as:
 * <ul>
 * <li>If the Request's view is set to "compact". then this will use the {@link ix.core.util.EntityUtils.EntityInfo}
 * of the model object to find any collapsed fields via {@link ix.core.util.EntityUtils.EntityInfo#getCollapsedFields()}
 * method and create links to the API with an href and count attribute.</li>
 * </ul>
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
            if(resource instanceof FieldResourceReference){
                String field = ((FieldResourceReference)resource).computedResourceLink();

                model.add(GsrsLinkUtil.fieldLink(action.getJsonFieldName(),entityLinks.linkFor(obj.getClass())
                        .slash(id)
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
                    model.add(
                            new CollectionFieldLink(((Collection) f.getValue(obj).get()).size(), field, linkTo(methodOn(model.getController()).getFieldById(id, Collections.emptyMap(), null)).withRel(compactFieldName).expand(id)));
                } else if (f.isArray()) {
                    model.add(
                            new CollectionFieldLink((Array.getLength(f.getValue(obj).get())), field, linkTo(methodOn(model.getController()).getFieldById(id, Collections.emptyMap(), null)).withRel(compactFieldName).expand(id)));
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

    private Link computeSelfLink(GsrsUnwrappedEntityModel<?> model, String id) {
        Link l= linkTo(methodOn(model.getController()).getById(id, Collections.emptyMap())).withRel("_self").expand(id);
        String query=l.toUri().getRawQuery();
        if(query==null){
            return l.withHref(l.getHref() +"?view=full");
        }
        return l.withHref(l.getHref() +"&view=full");


    }

}
