package gsrs.controller.hateoas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import gov.nih.ncats.common.util.CachedSupplier;
import ix.core.util.EntityUtils;
import lombok.Data;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    @Override
    public GsrsUnwrappedEntityModel<?> process(GsrsUnwrappedEntityModel<?> model) {
        Object obj = model.getObj();
        EntityUtils.EntityInfo<?> info = EntityUtils.EntityWrapper.of(obj).getEntityInfo();
        CachedSupplier<String> idString = info.getIdString(obj);
        String id = idString.get();
        //always add self
        model.add(computeSelfLink(model, id));

        if(model.isCompact()) {
            info.getCollapsedFields().forEach(f -> {
                String compactFieldName = f.getCompactViewFieldName();
                String field = f.getName();


                System.out.println(field);
                if (f.isCollection()) {
                    model.add(
                            new CollectionLink(((Collection) f.getValue(obj).get()).size(), field, linkTo(methodOn(model.getController()).getFieldById(id, Collections.emptyMap(), null)).withRel(compactFieldName).expand(id)));
                } else if (f.isArray()) {
                    model.add(
                            new CollectionLink((Array.getLength(f.getValue(obj).get())), field, linkTo(methodOn(model.getController()).getFieldById(id, Collections.emptyMap(), null)).withRel(compactFieldName).expand(id)));
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

    /**
     * A link that we have to modify ourselves
     * to generate the api/v1 part since
     * HATEOAS will see the Controller annotations
     * to get the rest but not our custom Mappers.
     */
    @Data
    private static class CollectionLink extends Link{

        private int count;
        @JsonIgnore //this is jsonIgnore because we delegate the getters with the same json annotations so ignoring means we don't list everything twice
        private Link link;


        CollectionLink(int count, String field, Link link){
            this.count = count;

            URI uri = link.toUri();
            //TODO add support for beyond v1.  maybe add version to the GsrsUnwrappedEntityModel ?
            String apiPath = "/api/v1" + uri.getRawPath().replace("/**", "/"+field);

            String host = uri.getHost();
            int port = uri.getPort();
            String scheme = uri.getScheme();
            StringBuilder apiBuilder = new StringBuilder(apiPath.length() + 20);
            if(scheme !=null){
                apiBuilder.append(scheme+"://");
            }
            apiBuilder.append(host==null?"localhost": host);
            if(port >=0){
                apiBuilder.append(":"+port);
            }

            apiBuilder.append(apiPath);
            this.link = link.withHref(apiBuilder.toString());
        }

        @Override
        public List<Affordance> getAffordances() {
            return link.getAffordances();
        }

        @Override
        public Link withSelfRel() {
            return link.withSelfRel();
        }

        @Override
        public Link andAffordance(Affordance affordance) {
            return link.andAffordance(affordance);
        }

        @Override
        public Link andAffordances(List<Affordance> affordances) {
            return link.andAffordances(affordances);
        }

        @Override
        public Link withAffordances(List<Affordance> affordances) {
            return link.withAffordances(affordances);
        }

        @Override
        @JsonIgnore
        public List<String> getVariableNames() {
            return link.getVariableNames();
        }

        @Override
        @JsonIgnore
        public List<TemplateVariable> getVariables() {
            return link.getVariables();
        }

        @Override
        public boolean isTemplated() {
            return link.isTemplated();
        }

        @Override
        public Link expand(Object... arguments) {
            return link.expand(arguments);
        }

        @Override
        public Link expand(Map<String, ?> arguments) {
            return link.expand(arguments);
        }

        @Override
        public Link withRel(LinkRelation relation) {
            return link.withRel(relation);
        }

        @Override
        public Link withRel(String relation) {
            return link.withRel(relation);
        }

        @Override
        public boolean hasRel(String rel) {
            return link.hasRel(rel);
        }

        @Override
        public boolean hasRel(LinkRelation rel) {
            return link.hasRel(rel);
        }

        @Override
        public URI toUri() {
            return link.toUri();
        }

        public static Link valueOf(String element) {
            return Link.valueOf(element);
        }

        @Override
        public Link withHref(String href) {
            return link.withHref(href);
        }

        @Override
        public Link withHreflang(String hreflang) {
            return link.withHreflang(hreflang);
        }

        @Override
        public Link withMedia(String media) {
            return link.withMedia(media);
        }

        @Override
        public Link withTitle(String title) {
            return link.withTitle(title);
        }

        @Override
        public Link withType(String type) {
            return link.withType(type);
        }

        @Override
        public Link withDeprecation(String deprecation) {
            return link.withDeprecation(deprecation);
        }

        @Override
        public Link withProfile(String profile) {
            return link.withProfile(profile);
        }

        @Override
        public Link withName(String name) {
            return link.withName(name);
        }

        @Override
        @JsonProperty
        public LinkRelation getRel() {
            return link.getRel();
        }

        @Override
        @JsonProperty
        public String getHref() {
            return link.getHref();
        }

        @Override
        @JsonProperty
        public String getHreflang() {
            return link.getHreflang();
        }

        @Override
        @JsonProperty
        public String getMedia() {
            return link.getMedia();
        }

        @Override
        @JsonProperty
        public String getTitle() {
            return link.getTitle();
        }

        @Override
        @JsonProperty
        public String getType() {
            return link.getType();
        }

        @Override
        @JsonProperty
        public String getDeprecation() {
            return link.getDeprecation();
        }

        @Override
        @JsonProperty
        public String getProfile() {
            return link.getProfile();
        }

        @Override
        @JsonProperty
        public String getName() {
            return link.getName();
        }

        @Override
        @JsonProperty
        public UriTemplate getTemplate() {
            return link.getTemplate();
        }
    }
}
