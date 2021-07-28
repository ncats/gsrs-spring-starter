package gsrs.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.repository.EditRepository;
import gsrs.service.AbstractGsrsEntityService;
import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import ix.core.controllers.EntityFactory;
import ix.core.models.Edit;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.pojopointer.PojoPointer;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidatorCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
//import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
//import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
//import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.ClassUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 *  Abstract GSRS Controller that generates all the
 *  standard GSRS routes in the form `api/v1/$context`;
 *  all entities that you wish to have routes that
 *  conform to the GSRS standard route paths
 *  should extend this class.
 *
 * @param <T> The entity type.
 * @param <I> the type for the entity's ID.
 *
 * @see GsrsRestApiController
 */
public abstract class AbstractGsrsEntityController<C extends AbstractGsrsEntityController, T, I> implements GsrsEntityController<T, I> {

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;


    @Autowired
    private EntityLinks entityLinks;

    @Autowired
    private ObjectMapper objectMapper;



//    /**
//     * Create a new GSRS Controller with the given context.
//     * @param context the context for the routes of this controller.
//     * @param idHelper the {@link IdHelper} to match an ID.  This will be used to determine
//     *                  Strings that are IDs versus Strings that should be flex looked up.  @see {@link #flexLookup(String)}.
//     * @throws NullPointerException if any parameters are null.
//     */
//    public AbstractGsrsEntityController(String context, IdHelper idHelper) {
//        this(context, idHelper.getPattern());
//    }
//    /**
//     * Create a new GSRS Controller with the given context.
//     * @param context the context for the routes of this controller.
//     * @param idPattern the {@link Pattern} to match an ID.  This will be used to determine
//     *                  Strings that are IDs versus Strings that should be flex looked up.  @see {@link #flexLookup(String)}.
//     * @throws NullPointerException if any parameters are null.
//     */
//    public AbstractGsrsEntityController(String context, Pattern idPattern) {
//        this.context = Objects.requireNonNull(context, "context can not be null");
//        this.idPattern = Objects.requireNonNull( idPattern, "id pattern can not be null");
//    }
//
//    @PostConstruct
//    private void initValidator(){
//        validatorFactory = CachedSupplier.runOnce(()->validatorFactoryService.newFactory(context));
//    }


    public GsrsControllerConfiguration getGsrsControllerConfiguration() {
        return gsrsControllerConfiguration;
    }

    protected EntityLinks getEntityLinks(){
        return entityLinks;
    }

    protected Optional<LinkBuilder> getLinkBuilderForEntity(Class entity){
        return GsrsLinkUtil.getEntityLinkForClassOrParentClass(entity, entityLinks);
    }
    protected abstract GsrsEntityService<T, I> getEntityService();

    //    @GetGsrsRestApiMapping("/{id:$ID}/index")
//    public void indexInfo(@PathVariable String id ){
//        Optional<T> t = get(parseIdFromString(id));
//        if(t.isPresent()){
//            new ReflectingIndexValueMaker().createIndexableValues(t.get(), iv->{
//                System.out.println("name = " + iv.name()+  " + path = " + iv.path() + " value =  " + iv.value());
//            });
//        }
//    }







                                   @Override
    @PostGsrsRestApiMapping()
    @Transactional
    public ResponseEntity<Object> createEntity(@RequestBody JsonNode newEntityJson,
                                               @RequestParam Map<String, String> queryParameters,
                                               Principal principal) throws IOException {
        System.out.println("injected Principal is " + principal);
        GsrsEntityService<T, I> entityService = getEntityService();
        System.out.println("found entityService " + entityService);
        AbstractGsrsEntityService.CreationResult<T> result = entityService.createEntity(newEntityJson);

        if(result.isCreated()){
            return new ResponseEntity<>(result.getCreatedEntity(), HttpStatus.CREATED);
        }

        return new ResponseEntity<>(result.getValidationResponse(),gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, queryParameters));


    }



    protected void addAdditionalLinks(GsrsUnwrappedEntityModel model){

    }


    @Override
    @PostGsrsRestApiMapping("/@validate")
    @Transactional(readOnly = true)
    public ValidationResponse<T> validateEntity(@RequestBody JsonNode updatedEntityJson, @RequestParam Map<String, String> queryParameters) throws Exception {

        
        ValidatorCategory vcat = Optional.ofNullable(queryParameters.get("category"))
                                         .map(term->ValidatorCategory.of(term))
                                         .orElse(ValidatorCategory.CATEGORY_ALL());
        
        ValidationResponse<T> resp = getEntityService().validateEntity(updatedEntityJson, vcat);
        //always send 200 even if validation has errors?
        return resp;

    }
    @Override
    @PutGsrsRestApiMapping("")
    @Transactional
    public ResponseEntity<Object> updateEntity(@RequestBody JsonNode updatedEntityJson,
                                               @RequestParam Map<String, String> queryParameters,
                                               Principal principal) throws Exception {

       AbstractGsrsEntityService.UpdateResult<T> result = getEntityService().updateEntity(updatedEntityJson);
        if(result.getStatus()== AbstractGsrsEntityService.UpdateResult.STATUS.NOT_FOUND){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }

       if(result.getStatus()== AbstractGsrsEntityService.UpdateResult.STATUS.ERROR){
           return new ResponseEntity<>(result.getValidationResponse(),gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, queryParameters));
        }

        //match 200 status of old GSRS
        return new ResponseEntity<>(result.getUpdatedEntity(), HttpStatus.OK);
    }

    @Override
    @GetGsrsRestApiMapping(value={"({id})/**", "/{id}/**" })
    public ResponseEntity<Object> getFieldById(@PathVariable("id") String id,
                                               @RequestParam(value="urldecode", required = false) Boolean urlDecode,
                                               @RequestParam Map<String, String> queryParameters, HttpServletRequest request) throws UnsupportedEncodingException {
        return returnOnySpecifiedFieldPartFor(getEntityService().getEntityBySomeIdentifier(id), urlDecode, queryParameters, request);
    }


    protected Optional<EditRepository> editRepository(){
        return Optional.empty();
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

    private ResponseEntity<Object> returnOnySpecifiedFieldPartFor(Optional<T> opt,
                                                                  @RequestParam("urldecode") Boolean urlDecode,
                                                                  @RequestParam Map<String, String> queryParameters, HttpServletRequest request) throws UnsupportedEncodingException {
        if(!opt.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        String field = GsrsControllerUtil.getEndWildCardMatchingPartOfUrl(request);
        //handle urldecode
        if(Boolean.TRUE.equals(urlDecode)){
            field =URLDecoder.decode(field, "UTF-8");
        }
        EntityUtils.EntityWrapper<T> ew = EntityUtils.EntityWrapper.of(opt.get());
        if(field !=null && field.startsWith("@edits")){
            Optional<EditRepository> editRepository = editRepository();
            if(editRepository.isPresent()){
                Optional<Object> nativeIdFor = ew.getEntityInfo().getNativeIdFor(opt.get());
                if(nativeIdFor.isPresent()){
                    List<Edit> editList = editRepository.get().findByRefidOrderByCreatedDesc(nativeIdFor.get().toString());
                    if(editList !=null) {
                        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView((List)editList, queryParameters, this::addAdditionalLinks), HttpStatus.OK);
                    }
                }

            }
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        if(field !=null){
            Optional<Object> fieldOpt = handleSpecialFields(ew, field);
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
            EntityWrapper ewv = at.get();
            
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



    @Override
    @GetGsrsRestApiMapping("/@count")
    public long getCount(){
        return getEntityService().count();
    }

    @Override
    @GetGsrsRestApiMapping("")
    @Transactional
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
        //match Gsrs Play API
        if(order ==null || order.trim().isEmpty()){
            return Sort.sort(getEntityService().getEntityClass());
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
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedResult<T>{
        private long total, count,skip, top;
        private Object content;

        public static PagedResult<EntityUtils.Key> ofKeys(Page<?> page){
            return PagedResult.<EntityUtils.Key>builder()
                   .total( page.getTotalElements())
                    .count(page.getNumberOfElements())
                    .skip( page.getSize() * page.getNumber())
                    .top( page.getSize())
                    .content( page.toList().stream().map(e->EntityUtils.EntityWrapper.of(e).getKey()).collect(Collectors.toList()))
                            .build();

        }
        public PagedResult(Page<T> page, Map<String, String> queryParameters){
            this.total = page.getTotalElements();
            this.count= page.getNumberOfElements();
            this.skip = page.getSize() * page.getNumber();
            this.top = page.getSize();
            content = page.toList().stream().map(e->GsrsControllerUtil.enhanceWithView(e, queryParameters)).collect(Collectors.toList());


        }
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
    @DeleteGsrsRestApiMapping(value = {"({id})", "/{id}"})
    public ResponseEntity<Object> deleteById(@PathVariable("id") String id, @RequestParam Map<String, String> queryParameters){
        Optional<I> idOptional = getEntityService().getEntityIdOnlyBySomeIdentifier(id);
        if(idOptional.isPresent()){
            getEntityService().delete(idOptional.get());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
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
                exists.setUrl(GsrsLinkUtil.computeSelfLinkFor(entityLinks, getEntityService().getEntityClass(),  idOptional.get().toString()));
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


//TODO katzelda October 2020 : for now delay work on modern hibernate search use legacy lucene

//    @Data
//    public static class LuceneSearchRequestField{
//        private String field;
//        private String matches;
//
//    }
//    @JsonTypeInfo(
//            use = JsonTypeInfo.Id.NAME,
//            property = "type",
//            defaultImpl = LuceneSearchRequestOr.class
//    )
//    @JsonSubTypes({
//            @JsonSubTypes.Type(value = LuceneSearchRequestOr.class, name = "or"),
//            @JsonSubTypes.Type(value = LuceneSearchRequestAnd.class, name = "and"),
//            @JsonSubTypes.Type(value = LuceneSearchRequestLeaf.class, name = "matches")
//    })
//    public interface LuceneSearchRequestOp {
//
//
//
//        PredicateFinalStep doIt(SearchPredicateFactory spf);
//    }
//    @Data
//    public class LuceneSearchRequest{
//        private LuceneSearchRequestOp op;
//
//        public PredicateFinalStep doIt(SearchPredicateFactory spf){
//            return op.doIt(spf);
//        }
//    }
//    @Data
//    public static class LuceneSearchRequestOr implements LuceneSearchRequestOp {
//        private List<LuceneSearchRequestOp> opList;
//
//        @Override
//        public PredicateFinalStep doIt(SearchPredicateFactory spf) {
//            BooleanPredicateClausesStep<?> step =spf.bool();
//            for(LuceneSearchRequestOp f : opList) {
//                step = step.should( f.doIt(spf));
//            }
//            return step;
//        }
//    }
//    @Data
//    public static class LuceneSearchRequestLeaf implements LuceneSearchRequestOp {
//        private LuceneSearchRequestField value;
//
//        @Override
//        public PredicateFinalStep doIt(SearchPredicateFactory spf) {
//
//            return spf.simpleQueryString().field(value.getField())
//                    .matching(value.getMatches());
//
//
//        }
//    }
//
//    @Data
//    public static class LuceneSearchRequestAnd implements LuceneSearchRequestOp {
//        private List<LuceneSearchRequestOp> opList;
//
//        @Override
//        public PredicateFinalStep doIt(SearchPredicateFactory spf) {
//            BooleanPredicateClausesStep<?> step =spf.bool();
//            for(LuceneSearchRequestOp f : opList) {
//                step = step.must(f.doIt(spf));
//            }
//            return step;
//        }
//    }





//    protected abstract List<T> indexSearchV2(LuceneSearchRequestOp op, Optional<Integer> top, Optional<Integer> skip, Optional<Integer> fdim);

//    @GsrsRestApiPostMapping(value = "/search", apiVersions = 2)
//    public ResponseEntity<Object> searchV2(@RequestBody LuceneSearchRequestOp op,
//                                           @RequestParam("top") Optional<Integer> top,
//                                           @RequestParam("skip") Optional<Integer> skip,
//                                           @RequestParam("fdim") Optional<Integer> fdim,
//                                           @RequestParam Map<String, String> queryParameters){
//
//        List<T> hits = indexSearchV2(op, top, skip, fdim);
//
//        //even if list is empty we want to return an empty list not a 404
//        return new ResponseEntity<>(hits, HttpStatus.OK);
//    }

    /*
      CREATE_OPERATION(new Operation("create")),
        VALIDATE_OPERATION(new Operation("validate")),
        //TODO: implement
        RESOLVE_OPERATION(new Operation("resolve",
                Argument.of(null, String.class, "id"))),
        UPDATE_ENTITY_OPERATION(new Operation("updateEntity")),
        PATCH_OPERATION(new Operation("patch",
                Argument.of(null, Id.class, "id"))),
        COUNT_OPERATION(new Operation("count")),
        STREAM_OPERATION(new Operation("stream",
                Argument.of(null, String.class, "field"),
                Argument.of(0, int.class , "top"),
                Argument.of(0, int.class , "skip"))),
        SEARCH_OPERATION(new Operation("search",
                Argument.of(null, String.class, "query"),
                Argument.of(0, int.class, "top"),
                Argument.of(0, int.class, "skip"),
                Argument.of(0, int.class, "fdim"))),
        GET_OPERATION(new Operation("get",
                Argument.of(null, Id.class, "id"),
                Argument.of(null, String.class, "expand"))),
        DELETE_OPERATION(new Operation("delete",
                Argument.of(null, Id.class, "id"))),
        DOC_OPERATION(new Operation("doc",
                Argument.of(null, Id.class, "id"))),
        EDITS_OPERATION(new Operation("edits",
                Argument.of(null, Id.class, "id"))),
        APPROVE_OPERATION(new Operation("approve",
                Argument.of(null, Id.class, "id"))),
        UPDATE_OPERATION(new Operation("update",
                Argument.of(null, Id.class, "id"),
                Argument.of(null, String.class, "field")

                )),
        FIELD_OPERATION(new Operation("field",
                Argument.of(null, Id.class, "id"),
                Argument.of(null, String.class, "field"))),
        PAGE_OPERATION(new Operation("page",
                Argument.of(10, int.class, "top"),
                Argument.of(0, int.class, "skip"),
                Argument.of(null, String.class, "filter"))),
        STRUCTURE_SEARCH_OPERATION(new Operation("structureSearch",
                Argument.of(null, String.class, "query"),
                Argument.of("substructure", String.class, "type"),
                Argument.of(.8, double.class, "cutoff"),
                Argument.of(0, int.class, "top"),
                Argument.of(0, int.class, "skip"),
                Argument.of(0, int.class, "fdim"),
                Argument.of("", String.class, "field"))),
        SEQUENCE_SEARCH_OPERATION(new Operation("sequenceSearch",
                Argument.of(null, String.class, "query"),
                Argument.of(CutoffType.SUB, CutoffType.class, "cutofftype"),
                Argument.of(.8, double.class, "cutoff"),
                Argument.of(0, int.class, "top"),
                Argument.of(0, int.class, "skip"),
                Argument.of(0, int.class, "fdim"),
                Argument.of("", String.class, "field"),
                Argument.of("", String.class, "seqType"))),


		HIERARCHY_OPERATION(new Operation("hierarchy",
				Argument.of(null, Id.class, "id"))),

		EXPORT_FORMATS_OPERATION(new Operation("exportFormats")),
		EXPORT_OPTIONS_OPERATION(new Operation("exportOptions",
				Argument.of(null, String.class, "etagId"),
				Argument.of(true, boolean.class, "publicOnly"))),
		EXPORT_OPERATION(new Operation("createExport",
				Argument.of(null, String.class, "etagId"),
				Argument.of(null, String.class, "format"),
				Argument.of(true, boolean.class, "publicOnly"))),
		;

     */
}
