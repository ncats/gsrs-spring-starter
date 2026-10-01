package gsrs.json;

import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.model.GsrsUrlLink;
import gsrs.springUtils.AutowireHelper;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkBuilder;

import java.util.Optional;

@JacksonComponent
public class GsrsUrlLinkSerializer extends ValueSerializer<GsrsUrlLink> {
    @Autowired
    private EntityLinks entityLinks;

    public GsrsUrlLinkSerializer() {
    }

    private synchronized void initIfNeeded(){
        if(entityLinks==null){
            AutowireHelper.getInstance().autowire(this);
        }
    }

    @Override
    public void serialize(GsrsUrlLink gsrsUrlLink, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
        if(gsrsUrlLink ==null){
            return;
        }
        initIfNeeded();
        Optional<LinkBuilder> optionalLinkBuilder = GsrsLinkUtil.getEntityLinkForClassOrParentClass(gsrsUrlLink.getEntityClass(), entityLinks);
        //there should be a found controller but unlikely event it doesn't don't throw an error
        if(optionalLinkBuilder.isPresent()) {
            LinkBuilder linkBuilder = optionalLinkBuilder.get()
                    .slash("(" + gsrsUrlLink.getId() + ")");
            if (gsrsUrlLink.getFieldPath() != null) {
                linkBuilder.slash(gsrsUrlLink.getFieldPath());
            }
            String uri = GsrsLinkUtil.fieldLink(gsrsUrlLink.getId(), gsrsUrlLink.getFieldPath(), linkBuilder.withSelfRel()).toUri().toString(); // this is a hack to fake the url we fix it downstream in the GsrsLinkUtil class
            jgen.writeString(uri);
        }
    }
}
