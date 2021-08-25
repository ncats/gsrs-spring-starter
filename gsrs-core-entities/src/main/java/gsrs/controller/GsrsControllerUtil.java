package gsrs.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModelProcessor;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.controllers.EntityFactory;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class GsrsControllerUtil {

    private GsrsControllerUtil(){
        //can not instantiate
    }

    public static String getRootUrlPath(){
        String url= StaticContextAccessor.getBean(EntityLinks.class).linkFor(RelativePathDummyObject.class).toUri().getRawPath();
        url.replace(RelativePathDummyObject.ROUTE_PATH,"");
        return url;
    }

    public static String getEndWildCardMatchingPartOfUrl(HttpServletRequest request) {
        //Spring boot can't use regex in path to get wildcard expression so have to use request
        ResourceUrlProvider urlProvider = (ResourceUrlProvider) request
                .getAttribute(ResourceUrlProvider.class.getCanonicalName());
        return urlProvider.getPathMatcher().extractPathWithinPattern(
                String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)),
                String.valueOf(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)));
    }
    public static GsrsUnwrappedEntityModel enhanceWithView(Object obj,  Map<String, String> queryParameters) {
        return enhanceWithView(obj, queryParameters, null);
    }

    public static GsrsUnwrappedEntityModel enhanceWithView(Object obj,  Map<String, String> queryParameters, Consumer<GsrsUnwrappedEntityModel> additionalLinksConsumer){
        String view = queryParameters.get("view");

        GsrsUnwrappedEntityModel model =  GsrsUnwrappedEntityModel.of(obj, view);
        //default view is compact
        if(view==null || "compact".equals(view)){
            model.setCompact(true);

        }

        if(model !=null && additionalLinksConsumer !=null) {
            additionalLinksConsumer.accept(model);
        }
        
        return StaticContextAccessor.getBean(GsrsUnwrappedEntityModelProcessor.class).process(model);
    }

    public static GsrsUnwrappedEntityModel enhanceWithView(List<Object> list, Map<String, String> queryParameters, Consumer<GsrsUnwrappedEntityModel> additionalLinksConsumer){
        List<Object> modelList = new ArrayList<>(list.size());
        for(Object o : list){
            modelList.add(enhanceWithView(o, queryParameters, additionalLinksConsumer));
        }
        return StaticContextAccessor.getBean(GsrsUnwrappedEntityModelProcessor.class).process(GsrsUnwrappedEntityModel.of(modelList, (String) null));
    }
}
