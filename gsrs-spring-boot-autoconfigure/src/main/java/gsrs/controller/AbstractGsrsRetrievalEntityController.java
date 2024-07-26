package gsrs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.repository.EditRepository;
import gsrs.service.GsrsEntityService;
import ix.core.EntityFetcher;
import ix.core.models.Edit;
import ix.core.util.EntityUtils;
import ix.core.util.pojopointer.PojoPointer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.Id;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class AbstractGsrsRetrievalEntityController<C extends AbstractGsrsRetrievalEntityController, T, I> implements GsrsRetrievalEntityController<T, I> {
    protected static Pattern VERSIONED_ROUTE = Pattern.compile("^v/(\\d+)$");
    @Autowired
    protected GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private EntityLinks entityLinks;

    public GsrsControllerConfiguration getGsrsControllerConfiguration() {
        return gsrsControllerConfiguration;
    }


    protected abstract GsrsEntityService<T, I> getEntityService();

    public EntityLinks getEntityLinks(){
        return entityLinks;
    }

    protected Optional<LinkBuilder> getLinkBuilderForEntity(Class entity){
        return GsrsLinkUtil.getEntityLinkForClassOrParentClass(entity, entityLinks);
    }

    protected void addAdditionalLinks(GsrsUnwrappedEntityModel model){

    }

    @Transactional(readOnly = true)
    @Override
    @GetGsrsRestApiMapping(value={"({id})/**", "/{id}/**" })
    public ResponseEntity<Object> getFieldById(@PathVariable("id") String id,
                                               @RequestParam(value="urldecode", required = false) Boolean urlDecode,
                                               @RequestParam Map<String, String> queryParameters, HttpServletRequest request) throws UnsupportedEncodingException {

        //This uses an entity fetcher instead of the existing lookup services
        //since entity fetchers tend to be faster
        Optional<T> ent = getEntityService()
                .getEntityIdOnlyBySomeIdentifier(id)
                .map(ii->(I)ii)
                .map(ii->{
                    EntityUtils.Key k= EntityUtils.Key.of(getEntityService().getEntityClass(), (Object)ii);
                    return k;
                })
                .map(k-> EntityFetcher.of(k).getIfPossible())
                .filter(ff->ff.isPresent())
                .map(f->(T)f.get());
//    	Old way
//    	Optional<T> ent = getEntityService().getEntityBySomeIdentifier(id);

        return returnOnySpecifiedFieldPartFor(ent, urlDecode, queryParameters, request);
    }

    private ResponseEntity<Object> returnOnySpecifiedFieldPartFor(Optional<T> opt,
                                                                  @RequestParam("urldecode") Boolean urlDecode,
                                                                  @RequestParam Map<String, String> queryParameters, HttpServletRequest request) throws UnsupportedEncodingException {
        if(!opt.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        String field = GsrsControllerUtil.getEndWildCardMatchingPartOfUrl(request);
        //handle urldecode
        if(Boolean.TRUE.equals(urlDecode)){
            field = URLDecoder.decode(field, "UTF-8");
        }
        EntityUtils.EntityWrapper<T> ew = EntityUtils.EntityWrapper.of(opt.get());

        if(field !=null){
            Optional<Object> fieldOpt =  handleFields(ew,field);
            if(fieldOpt !=null){
                if(fieldOpt.isPresent()){
                    Object o = fieldOpt.get();
                    if(o instanceof List){
                        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView((List) o, queryParameters, this::addAdditionalLinks), HttpStatus.OK);

                    }else {
                        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(o, queryParameters, this::addAdditionalLinks), HttpStatus.OK);
                    }
                }else{
                    return gsrsControllerConfiguration.handleNotFound(queryParameters);
                }
            }
        }
        PojoPointer pojoPointer = PojoPointer.fromURIPath(field);

        Optional<EntityUtils.EntityWrapper<?>> at = ew.at(pojoPointer);
        if(!at.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        //match old Play version of GSRS which either return JSON for an object or raw string?

        if(pojoPointer.isLeafRaw() /*|| at.get().getRawValue() instanceof String */){
            Object rawValue = at.get().getRawValue();
            //force raw value to be String
            if(rawValue !=null && !(rawValue instanceof String)){
                rawValue = rawValue.toString();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(rawValue);

        }else{
            EntityUtils.EntityWrapper ewv = at.get();

            if(!ewv.isEntity()) {
                JsonNode jsn=ewv.toFullJsonNode();
                if(jsn.isValueNode()) {
                    return new ResponseEntity<>(jsn, HttpStatus.OK);
                }
            }
//            boolean isPrimitiveOrWrapped = (value!=null)?
//                    ClassUtils.isPrimitiveOrWrapper(value.getClass())|| value instanceof String:true;
//
//            if(isPrimitiveOrWrapped){
//                //just a plain String - no links?
//                //if we pass it to the enhance view below it will error out
//                Map<String,Object> wrapMap = new HashMap<>();
//                wrapMap.put("value",value);
//                JsonNode json;
//                JsonNode jsonwrap = objectMapper.valueToTree(wrapMap);
//                json = jsonwrap.get("value");
//                return new ResponseEntity<>(json, HttpStatus.OK);
//                //return new ResponseEntity<>(value, HttpStatus.OK);
//            }
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(ewv.getValue(), queryParameters, this::addAdditionalLinks), HttpStatus.OK);
        }
    }

    protected Optional<EditRepository> editRepository(){
        return Optional.empty();
    }

    protected Optional<Object> handleVersion(EntityUtils.EntityWrapper<T> ew, String field){
        Matcher m = VERSIONED_ROUTE.matcher(field);
        if(m.find()){
            Optional<EditRepository> editRepository = editRepository();
            if(editRepository.isPresent()){
                Optional<Object> nativeIdFor = ew.getEntityInfo().getNativeIdFor(ew.getValue());
                if(nativeIdFor.isPresent()) {
                    List<Edit> editList = editRepository.get().findByRefidAndVersion(nativeIdFor.get().toString(), m.group(1));
                    if(editList!=null && !editList.isEmpty()){
                        try {
                            return Optional.of(ew.getEntityInfo().fromJson(editList.get(0).newValue));
                        } catch (IOException e) {
                            log.error("error fetching edit from json", e);
                        }
                    }
                }
            }
            return Optional.empty();
        }
        return null;
    }

    /**
     * Handle special field mappings, this is where any special custom API fields (Like "@hierarchy for Substances)
     * can go.
     * @param entity the Entity to work on.
     * @param field the field name;
     * @return {@code null} if you don't handle this special field, Optional empty
     * if you do handle this field and couldn't find it (so we can return a 404 type response) and
     * an non-null Object inside the Optional to handle the field.
     */
    protected Optional<Object> handleSpecialFields(EntityUtils.EntityWrapper<T> entity, String field){
        return null;
    }

    protected Optional<Object> handleFields(EntityUtils.EntityWrapper<T> entity, String field){
        List<BiFunction<EntityUtils.EntityWrapper<T>, String, Optional<Object>>> functions = Arrays.asList(
                this::handleVersion
        );
        for(BiFunction<EntityUtils.EntityWrapper<T>, String, Optional<Object>> function : functions){
            Optional<Object> opt = function.apply(entity, field);
            if(opt !=null) {
                return opt;

            }
        }

        return handleSpecialFields(entity, field);
    }

    @Override
    @PostGsrsRestApiMapping("/@exists")
    public ExistsCheckResult entitiesExists(@RequestBody List<String> idList, @RequestParam Map<String, String> queryParameters) throws Exception{
        Map<String, EntityExists> foundMap = new LinkedHashMap<>();
        List<String> notFound = new ArrayList<>();
        for(String id : idList){
            Optional<I> idOptional = getEntityService().getEntityIdOnlyBySomeIdentifier(id);
            if(idOptional.isPresent()){
                EntityExists exists = new EntityExists();
                exists.setId(idOptional.get().toString());
                exists.setQuery(id);
                exists.setUrl(GsrsLinkUtil.computeSelfLinkFor(entityLinks, getEntityService().getEntityClass(),  idOptional.get().toString()).getHref());
                foundMap.put(id, exists);
            }else{
                notFound.add(id);
            }

        }
        ExistsCheckResult result = new ExistsCheckResult();
        result.setFound(foundMap);
        result.setNotFound(notFound);

        return result;
    }

    @Override
    @GetGsrsRestApiMapping(value = {"({id})", "/{id}" })
    public ResponseEntity<Object> getById(@PathVariable("id") String id, @RequestParam Map<String, String> queryParameters){
        Optional<T> obj = getEntityService().getEntityBySomeIdentifier(id);
        if(obj.isPresent()){
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(obj.get(), queryParameters, this::addAdditionalLinks), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @Override
    @GetGsrsRestApiMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<Object> page(@RequestParam(value = "top", defaultValue = "10") long top,
                                       @RequestParam(value = "skip", defaultValue = "0") long skip,
                                       @RequestParam(value = "order", required = false) String order,
                                       @RequestParam Map<String, String> queryParameters){


        Page<T> page = getEntityService().page(new OffsetBasedPageRequest(skip, top,parseSortFromOrderParam(order)));

        String view=queryParameters.get("view");
        if("key".equals(view)){
            return new ResponseEntity<>( PagedResult.ofKeys(page), HttpStatus.OK);

        }
        return new ResponseEntity<>(new PagedResult(page, queryParameters), HttpStatus.OK);
    }

    private Sort parseSortFromOrderParam(String order){
        if(order == null || order.trim().isEmpty()){
            Field[] fields = getEntityService().getEntityClass().getFields();

            boolean found = false;
            String name = "";
            for(Field field: fields) {
                if(found)
                    break;
                name = field.getName();
                Annotation[] annotations = field.getAnnotations();
                if(annotations.length > 0) {
                    for(Annotation annotation : annotations) {
                        if(annotation.annotationType().equals(Id.class)) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            return Sort.by(Sort.Direction.ASC, name);
        }
        char firstChar = order.charAt(0);
        if('$'==firstChar){
            return Sort.by(Sort.Direction.DESC, order.substring(1));
        }
        if('^'==firstChar){
            return Sort.by(Sort.Direction.ASC, order.substring(1));
        }
        return Sort.by(Sort.Direction.ASC, order);
    }

}
