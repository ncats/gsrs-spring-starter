package gsrs.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import gsrs.controller.hateoas.GsrsEntityToControllerMapper;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModelProcessor;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.controllers.EntityFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public final class GsrsControllerUtil {

    private static final Map<String, String> FULL_VIEW= Collections.singletonMap("view", "full");
    private GsrsControllerUtil(){
        //can not instantiate
    }

    /**
     * Get the HATEOS proxied class for a mock invocation
     * of the controller for the given entity class.
     *
     * @apiNote This is the same as {@link WebMvcLinkBuilder#methodOn(Class, Object...)}
     * using the Controller class returned by {@link GsrsEntityToControllerMapper#getControllerFor(Class)}
     * using the passed in entity Class.
     * @param entityClass the entity to look for the controlelr for.
     * @param <T> the type of the expected Controller so we can invoke mock methods
     *           on it.
     * @return a proxied instance to invoke mock methods on it to compute URLs
     * or {@code null} if no controller is found.
     */
    public static <T> T methodOnControllerFor(Class<?> entityClass){
        GsrsEntityToControllerMapper mapper = StaticContextAccessor.getBean(GsrsEntityToControllerMapper.class);
        if(mapper ==null){
            return null;
        }
        Optional<Class> controller = mapper.getControllerFor(entityClass);
        if(controller.isPresent()){
            return WebMvcLinkBuilder.methodOn((Class<T>)controller.get());
        }
        return null;
    }

    public static String getRootUrlPath(){
        String url = WebMvcLinkBuilder.linkTo(RelativePathDummyObject.class).toUri().getRawPath();
//        LinkBuilder linkBuilder = StaticContextAccessor.getBean(EntityLinks.class).linkFor(RelativePathDummyObject.class);
//        URI uri = linkBuilder.toUri();
//        String url= uri.getRawPath();
        String replaced = url.replace(RelativePathDummyObject.ROUTE_PATH,"");
        return replaced;
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
    public static GsrsUnwrappedEntityModel enhanceWithViewFull(Object obj) {
        return enhanceWithView(obj, FULL_VIEW, null);
    }

    public static GsrsUnwrappedEntityModel enhanceWithView(Object obj,  Map<String, String> queryParameters, Consumer<GsrsUnwrappedEntityModel> additionalLinksConsumer){
        String view = queryParameters.get("view");
        //log.trace("in enhanceWithView: " + view);

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
        //log.trace("enhanceWithView with list");
        List<Object> modelList = new ArrayList<>(list.size());
        for(Object o : list){
            modelList.add(enhanceWithView(o, queryParameters, additionalLinksConsumer));
        }
        return StaticContextAccessor.getBean(GsrsUnwrappedEntityModelProcessor.class).process(GsrsUnwrappedEntityModel.of(modelList, (String) null));
    }
}
