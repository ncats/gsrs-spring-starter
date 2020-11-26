package gsrs.controller;


import com.fasterxml.jackson.databind.JsonNode;
import gsrs.service.AbstractGsrsEntityService;
import gsrs.service.GsrsEntityService;
import ix.core.util.EntityUtils;
import ix.core.util.pojopointer.PojoPointer;
import ix.core.validator.ValidationResponse;
import lombok.Data;
//import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
//import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
//import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import java.util.Optional;


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
public abstract class AbstractGsrsEntityController<T, I> {

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private GsrsEntityService<T, I> entityService;

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

    public GsrsEntityService<T, I> getEntityService() {
        return entityService;
    }

    //    @GetGsrsRestApiMapping("/{id:$ID}/index")
//    public void indexInfo(@PathVariable String id ){
//        Optional<T> t = get(parseIdFromString(id));
//        if(t.isPresent()){
//            new ReflectingIndexValueMaker().createIndexableValues(t.get(), iv->{
//                System.out.println("name = " + iv.name()+  " + path = " + iv.path() + " value =  " + iv.value());
//            });
//        }
//    }
    @PostGsrsRestApiMapping()
    public ResponseEntity<Object> createEntity(@RequestBody JsonNode newEntityJson, @RequestParam Map<String, String> queryParameters) throws IOException {
        AbstractGsrsEntityService.CreationResult<T> result = entityService.createEntity(newEntityJson);

        if(result.isCreated()){
            return new ResponseEntity<>(result.getCreatedEntity(), HttpStatus.CREATED);
        }

        return new ResponseEntity<>(result.getValidationResponse(),gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, queryParameters));


    }

    /*private void foo(){
        ValidationResponseBuilder callback = new ValidationUtils.GinasValidationResponseBuilder(objnew, _strategy);

        //turn off duplicate checking in batch mode
        if(this.method == METHOD_TYPE.BATCH){
            callback.allowPossibleDuplicates(true);
        }

        this.validate(objnew, objold, callback);
        ValidationResponse<Substance> resp =  callback.buildResponse();

        List<GinasProcessingMessage> messages = resp.getValidationMessages()
                .stream()
                .filter(m-> m instanceof GinasProcessingMessage)
                .map(m ->(GinasProcessingMessage)m)
                .collect(Collectors.toList());
        messages.stream().forEach( _strategy::processMessage);
        if(_strategy.handleMessages(objnew, messages)){
            resp.setValid();
        }
        _strategy.addProblems(objnew, messages);



        if(GinasProcessingMessage.ALL_VALID(messages)){
            resp.addValidationMessage(GinasProcessingMessage.SUCCESS_MESSAGE("Substance is valid"));
        }
        return resp;


    }

     */
    @PostGsrsRestApiMapping("/@validate")
    public ValidationResponse<T> validateEntity(@RequestBody JsonNode updatedEntityJson, @RequestParam Map<String, String> queryParameters) throws Exception {

        ValidationResponse<T> resp = entityService.validateEntity(updatedEntityJson);
        //always send 200 even if validation has errors?
        return resp;

    }
    @PutGsrsRestApiMapping()
    public ResponseEntity<Object> updateEntity(@RequestBody JsonNode updatedEntityJson, @RequestParam Map<String, String> queryParameters) throws Exception {

       AbstractGsrsEntityService.UpdateResult<T> result = entityService.updateEntity(updatedEntityJson);
        if(result.getStatus()== AbstractGsrsEntityService.UpdateResult.STATUS.NOT_FOUND){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }

       if(result.getStatus()== AbstractGsrsEntityService.UpdateResult.STATUS.ERROR){
           return new ResponseEntity<>(result.getValidationResponse(),gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, queryParameters));
        }

        //match 200 status of old GSRS
        return new ResponseEntity<>(result.getUpdatedEntity(), HttpStatus.OK);
    }

    @GetGsrsRestApiMapping(value={"/{id}/**", "({id})/**" })
    public ResponseEntity<Object> getFieldById(@PathVariable String id, @RequestParam Map<String, String> queryParameters, HttpServletRequest request){
        return returnOnySpecifiedFieldPartFor(entityService.getEntityBySomeIdentifier(id), queryParameters, request);
    }





    private ResponseEntity<Object> returnOnySpecifiedFieldPartFor(Optional<T> opt, @RequestParam Map<String, String> queryParameters, HttpServletRequest request) {
        if(!opt.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        String field = getEndWildCardMatchingPartOfUrl(request);

        PojoPointer pojoPointer = PojoPointer.fromURIPath(field);
        Optional<EntityUtils.EntityWrapper<?>> at = EntityUtils.EntityWrapper.of(opt.get()).at(pojoPointer);
        if(!at.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        //match old Play version of GSRS which either return JSON for an object or raw string?
        EntityUtils.EntityWrapper ew= at.get();
        if(pojoPointer.isLeafRaw()){
            return new ResponseEntity<>(ew.getRawValue(), HttpStatus.OK);
        }else{
            return new ResponseEntity<>(ew.toFullJsonNode(), HttpStatus.OK);
        }
    }

    private static String getEndWildCardMatchingPartOfUrl(HttpServletRequest request) {
        //Spring boot can't use regex in path to get wildcard expression so have to use request
        ResourceUrlProvider urlProvider = (ResourceUrlProvider) request
                .getAttribute(ResourceUrlProvider.class.getCanonicalName());
        return urlProvider.getPathMatcher().extractPathWithinPattern(
                String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)),
                String.valueOf(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)));
    }

    @GetGsrsRestApiMapping("/@count")
    public long getCount(){
        return entityService.count();
    }

    @GetGsrsRestApiMapping("")
    public ResponseEntity<Object> page(@RequestParam(value = "top", defaultValue = "16") long top,
                     @RequestParam(value = "skip", defaultValue = "0") long skip,
                     @RequestParam(value = "order", required = false) String order,
                     @RequestParam Map<String, String> queryParameters){


        Page<T> page = entityService.page(new OffsetBasedPageRequest(skip, top,parseSortFromOrderParam(order)));

        return new ResponseEntity<>(new PagedResult(page), HttpStatus.OK);
    }

    private Sort parseSortFromOrderParam(String order){
        //match Gsrs Play API
        if(order ==null || order.trim().isEmpty()){
            return Sort.sort(entityService.getEntityClass());
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
    public static class PagedResult<T>{
        private long total, count,skip, top;
        private List<T> content;

        public PagedResult(Page<T> page){
            this.total = page.getTotalElements();
            this.count= page.getNumberOfElements();
            this.skip = page.getSize() * page.getNumber();
            this.top = page.getSize();
            content = page.toList();
        }
    }

    @GetGsrsRestApiMapping(value = {"/{id}", "({id})"})
    public ResponseEntity<Object> getById(@PathVariable String id, @RequestParam Map<String, String> queryParameters){
        Optional<T> obj = entityService.getEntityBySomeIdentifier(id);
        if(obj.isPresent()){
            return new ResponseEntity<>(obj.get(), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @DeleteGsrsRestApiMapping(value = {"/{id}", "({id})"})
    public ResponseEntity<Object> deleteById(@PathVariable String id, @RequestParam Map<String, String> queryParameters){
        Optional<I> idOptional = entityService.getEntityIdOnlyBySomeIdentifier(id);
        if(idOptional.isPresent()){
            entityService.delete(idOptional.get());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
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
