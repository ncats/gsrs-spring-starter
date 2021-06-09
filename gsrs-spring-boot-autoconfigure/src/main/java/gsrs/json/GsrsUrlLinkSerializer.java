package gsrs.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.model.GsrsUrlLink;
import gsrs.springUtils.AutowireHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkBuilder;

import java.io.IOException;
@JsonComponent
public class GsrsUrlLinkSerializer extends JsonSerializer<GsrsUrlLink> {
    @Autowired
    private EntityLinks entityLinks;


    private synchronized void initIfNeeded(){
        if(entityLinks==null){
            AutowireHelper.getInstance().autowire(this);
        }
    }

    @Override
    public void serialize(GsrsUrlLink gsrsUrlLink, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if(gsrsUrlLink ==null){
            return;
        }
        initIfNeeded();
        LinkBuilder linkBuilder = entityLinks.linkFor(gsrsUrlLink.getEntityClass())
                .slash("(" + gsrsUrlLink.getId() + ")");
        if(gsrsUrlLink.getFieldPath() !=null){
            linkBuilder.slash(gsrsUrlLink.getFieldPath());
        }
        String uri = GsrsLinkUtil.fieldLink(gsrsUrlLink.getId(), gsrsUrlLink.getFieldPath(), linkBuilder.withSelfRel()).toUri().toString(); // this is a hack to fake the url we fix it downstream in the GsrsLinkUtil class
        provider.defaultSerializeValue(uri, jgen);
    }
}
