package gsrs.controller.hateoas;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gsrs.controller.GsrsControllerUtil;
import gsrs.springUtils.StaticContextAccessor;

import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.UriTemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A link that we have to modify ourselves
 * to generate the api/v1 part since
 * HATEOAS will see the Controller annotations
 * to get the rest but not our custom Mappers.
 */
@Data
@EqualsAndHashCode(callSuper=false)
class FieldLink extends Link implements GsrsCustomLink{
    @JsonIgnore
    //this is jsonIgnore because we delegate the getters with the same json annotations so ignoring means we don't list everything twice
    private Link link;


    FieldLink(String field, Link link, String id){
        URI uri = link.toUri();
//        System.out.println("orl url =" + uri);
        //TODO add support for beyond v1.  maybe add version to the GsrsUnwrappedEntityModel ?
        String rootPath = GsrsControllerUtil.getRootUrlPath();
        String currentPath = uri.getRawPath();
        String downStream;
        if(rootPath.trim().isEmpty()){
            downStream = currentPath;
        }else {
            //the root should be a subString of the currentPath...
            downStream = ("$" + currentPath).replace("$" + rootPath, "");
        }
        
        String apiPath;

        if(!downStream.startsWith("/")){
            downStream="/"+downStream;
        }
        if(id !=null) {
            apiPath = "/api/v1" + downStream
                    .replace("/(" + id + ")", "(" + id + ")");
        }else{
            apiPath = "/api/v1" + downStream;
        }
        if(field !=null){
            apiPath = apiPath.replace("/**", "/"+field);
        }
        
        //GSRS api sometimes uses format $controller($id)
        //instead of $controller/$id
        //HATEOAS adds the slash to make it $controller/($id) so we have to fix it
        String query = uri.getRawQuery();
        if(query !=null){
            apiPath +="?"+query;
        }

        StringBuilder apiBuilder = new StringBuilder(apiPath.length() + 20);
        
        String configHostAndPort = StaticContextAccessor.getBean(IxContext.class).getEffectiveHostURI();
        apiBuilder.append(configHostAndPort);
        
        //factored this part out
        
//        if(configHostAndPort==null) {
//            String host = uri.getHost();
//            int port = uri.getPort();
//            String scheme = uri.getScheme();
//            if(scheme !=null){
//                apiBuilder.append(scheme+"://");
//            }
//            apiBuilder.append(host==null?"localhost": host);
//            if(port >=0){
//                apiBuilder.append(":"+port);
//            }
//        }else {
//            apiBuilder.append(configHostAndPort);
//        }

        apiBuilder.append(apiPath);
        this.link = link.withHref(apiBuilder.toString());
    }

    @Override
    public Map<String, Object> getCustomSerializedProperties() {
        Map<String, Object> map = new LinkedHashMap<>();
        return map;
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
