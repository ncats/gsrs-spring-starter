package gsrs.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.validator.ValidatorFactoryService;
import ix.core.util.EntityUtils;
import ix.core.util.pojopointer.PojoPointer;
import ix.core.validator.ValidationResponse;
import ix.core.validator.Validator;
import ix.ginas.utils.validation.ValidatorFactory;
import ix.utils.pojopatch.PojoDiff;
import ix.utils.pojopatch.PojoPatch;
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

import javax.annotation.PostConstruct;
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
    private ValidatorFactoryService validatorFactoryService;

    @Autowired
    private ObjectMapper mapper;


    private final String context;

    private CachedSupplier<ValidatorFactory> validatorFactory;

    /**
     * Create a new GSRS Controller with the given context.
     * @param context
     */
    public AbstractGsrsEntityController(String context) {
        this.context = context;
    }

    @PostConstruct
    private void initValidator(){
        validatorFactory = CachedSupplier.runOnce(()->validatorFactoryService.newFactory(context, mapper));
    }

    /**
     * Create a new instance of your entity type from the provided JSON.
     * Perform any object clean up or "data cleansing" here.
     *
     * @param json the JSON to use; will never be null.
     *
     * @return a new entity instance will never be null.
     *
     * @throws IOException if there's a problem processing the JSON.
     */
    protected abstract T fromNewJson(JsonNode json) throws IOException;
    /**
     * Create List of  new instances of your entity type from the provided JSON which
     * is a List of entities.
     * Perform any object clean up or "data cleansing" here.
     *
     * @param list the JSON to use; will never be null.
     *
     * @return a new List of instances will never be null and probably shouldn't be empty.
     *
     * @throws IOException if there's a problem processing the JSON.
     */
    protected abstract List<T> fromNewJsonList(JsonNode list) throws IOException;
    /**
     * Create a instance of your entity type from the provided JSON representing
     * an updated version of a  record.
     * Perform any object clean up or "data cleansing" here but keep in mind different data cleansings
     * might be done on new vs updated records.
     *
     * @param json the JSON to use; will never be null.
     *
     * @return a new entity instance will never be null.
     *
     * @throws IOException if there's a problem processing the JSON.
     */
    protected abstract T fromUpdatedJson(JsonNode json) throws IOException;
    /**
     * Create List of  instances of your entity type from the provided JSON which
     * is a List of updated versions of records.
     * Perform any object clean up or "data cleansing" here  here but keep in mind different data cleansings
     * might be done on new vs updated records.
     *
     * @param list the JSON to use; will never be null.
     *
     * @return a new List of instances will never be null and probably shouldn't be empty.
     *
     * @throws IOException if there's a problem processing the JSON.
     */
    protected abstract List<T> fromUpdatedJsonList(JsonNode list) throws IOException;

    /**
     * Create a JSON representation of your entity.
     * @param t
     * @return
     * @throws IOException
     */
    protected abstract JsonNode toJson(T t) throws IOException;

    /**
     * Save the given entity to your data repository.
     * @param t the entity to save.
     * @return the new saved entity, usually this is the same object as the t passed in but it doesn't have to be.
     * The returned object may have different fields set like id, or audit information etc.
     */
    protected abstract T create(T t);

    /**
     * Get the number of entities in your data repository.
     * @return a number &ge;0.
     */
    protected abstract long count();

    /**
     * Get the entity from your data repository with the given id.
     * @param id the id to use to look up the entity.
     * @return an Optional that is empty if there is no entity with the given id;
     * or an Optional that has the found entity.
     */
    protected abstract Optional<T> get(I id);

    /**
     * Parse the Id from the given String.
     * @param idAsString the String to parse into an ID.
     * @return the ID as the correct type.
     */
    protected abstract I parseIdFromString(String idAsString);

    /**
     * Fetch an entity from the data repository using a unique
     * identifier that isn't the entity's ID.
     *
     * @param someKindOfId a String that isn't the ID of the entity.
     * @return an Optional that is empty if there is no entity with the given id;
     *  or an Optional that has the found entity.
     */
    protected abstract Optional<T> flexLookup(String someKindOfId);
    /**
     * Fetch an entity's real ID from the data repository using a unique
     * identifier that isn't the entity's ID.  Override this method
     * if a more effienient query can be made rather than fetching the whole record.
     *
     * @implSpec the default implementation of this method is:
     * <pre>
     * {@code
     *  Optional<T> opt = flexLookup(someKindOfId);
     *  if(!opt.isPresent()){
     *     return Optional.empty();
     *  }
     *  return Optional.of(getIdFrom(opt.get()));
     * }
     * </pre>
     * @param someKindOfId a String that isn't the ID of the entity.
     * @return an Optional that is empty if there is no entity with the given id;
     *  or an Optional that has the found entity's ID.
     */
    protected Optional<I> flexLookupIdOnly(String someKindOfId){
        Optional<T> opt = flexLookup(someKindOfId);
        if(!opt.isPresent()){
            return Optional.empty();
        }
        return Optional.of(getIdFrom(opt.get()));
    }

    /**
     * Get the Class of the Entity.
     * @return a Class; will never be {@code null}.
     */
    protected abstract Class<T> getEntityClass();

    /**
     * Return a {@link Page} of entities from the repository using the
     * given offset, num records and sort order.
     * @param offset the number of records to start at ( 0-based).
     * @param numOfRecords the number of records to include in the page.
     * @param sort the {@link Sort} to use.
     * @return the Page from the repository.
     *
     * @see OffsetBasedPageRequest
     */
    protected abstract Page page(long offset, long numOfRecords, Sort sort);

    /**
     * Remove the given entity from the repository.
     * @param id the id of the entity to delete.
     */
    protected abstract void delete(I id);

    /**
     * Get the ID from the entity.
     * @param entity the entity to get the id of.
     * @return the ID of this entity.
     */
    //TODO should this be turned into a Function so we can pass a method reference in constructor?
    protected abstract I getIdFrom(T entity);

    /**
     * Update the given entity in the repository.
     * @param t the entity to update.
     * @return the new saved entity, usually this is the same object as the t passed in but it doesn't have to be.
     * The returned object may have different fields set like  audit information etc.
     */
    protected abstract T update(T t);

    /**
     * Get the {@link GsrsControllerConfiguration} that was injected into this controller.
     * @return
     */
    protected final GsrsControllerConfiguration getGsrsControllerConfiguration(){
        return gsrsControllerConfiguration;
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
    public ResponseEntity<Object> createEntity(@RequestBody JsonNode newEntityJson) throws IOException {
        T newEntity = fromNewJson(newEntityJson);

        Validator<T> validator  = validatorFactory.getSync().createValidatorFor(newEntity, null, ValidatorFactoryService.ValidatorConfig.METHOD_TYPE.CREATE);
        ValidationResponse<T> resp = validator.validate(newEntity, null);
        if(resp!=null && !resp.isValid()){
            return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(create(newEntity), HttpStatus.CREATED);

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
    public ResponseEntity<Object> validateEntity(@RequestBody JsonNode updatedEntityJson, @RequestParam Map<String, String> queryParameters) throws Exception {
        T updatedEntity = fromUpdatedJson(updatedEntityJson);
        //updatedEntity should have the same id
        I id = getIdFrom(updatedEntity);
        Optional<T> opt = get(id);
        Validator<T> validator;
        if(opt.isPresent()){
            validator  = validatorFactory.getSync().createValidatorFor(updatedEntity, opt.get(), ValidatorFactoryService.ValidatorConfig.METHOD_TYPE.UPDATE);

        }else{
            validator  = validatorFactory.getSync().createValidatorFor(updatedEntity, null, ValidatorFactoryService.ValidatorConfig.METHOD_TYPE.CREATE);

        }
        ValidationResponse<T> resp = validator.validate(updatedEntity, opt.orElse(null));

        //always send 200 even if validation has errors?
        return new ResponseEntity<>(resp, HttpStatus.OK);

    }
        @PutGsrsRestApiMapping()
    public ResponseEntity<Object> updateEntity(@RequestBody JsonNode updatedEntityJson, @RequestParam Map<String, String> queryParameters) throws Exception {
        T updatedEntity = fromUpdatedJson(updatedEntityJson);
        //updatedEntity should have the same id
        I id = getIdFrom(updatedEntity);
        Optional<T> opt = get(id);
        if(!opt.isPresent()){
            return gsrsControllerConfiguration.handleBadRequest(queryParameters);
        }

        T oldEntity = opt.get();

        Validator<T> validator  = validatorFactory.getSync().createValidatorFor(updatedEntity, oldEntity, ValidatorFactoryService.ValidatorConfig.METHOD_TYPE.CREATE);
        ValidationResponse<T> resp = validator.validate(updatedEntity, oldEntity);
        if(resp!=null && !resp.isValid()){
            return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
        }

        PojoPatch<T> patch = PojoDiff.getDiff(oldEntity, updatedEntity);
        System.out.println("changes = " + patch.getChanges());
        patch.apply(oldEntity);
        System.out.println("updated entity = " + oldEntity);

        //match 200 status of old GSRS
        return new ResponseEntity<>(update(oldEntity), HttpStatus.OK);
    }

    @GetGsrsRestApiMapping(value={"/{id:$ID}/**", "({id:$ID})/**" })
    public ResponseEntity<Object> getFieldById(@PathVariable String id, @RequestParam Map<String, String> queryParameters, HttpServletRequest request){
        Optional<T> opt = get(parseIdFromString(id));
        return returnOnySpecifiedFieldPartFor(opt, queryParameters, request);
    }

    @GetGsrsRestApiMapping(value={"/{id:$NOT_ID}/**", "({id:$NOT_ID})/**" })
    public ResponseEntity<Object> getFieldByFlex(@PathVariable String someKindOfId, @RequestParam Map<String, String> queryParameters, HttpServletRequest request){
        Optional<T> opt = flexLookup(someKindOfId);
        return returnOnySpecifiedFieldPartFor(opt, queryParameters, request);
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
        return count();
    }

    @GetGsrsRestApiMapping("")
    public ResponseEntity<Object> page(@RequestParam(value = "top", defaultValue = "16") long top,
                     @RequestParam(value = "skip", defaultValue = "0") long skip,
                     @RequestParam(value = "order", required = false) String order,
                     @RequestParam Map<String, String> queryParameters){


        Page<T> page = page(skip, top,parseSortFromOrderParam(order));

        return new ResponseEntity<>(new PagedResult(page), HttpStatus.OK);
    }

    private Sort parseSortFromOrderParam(String order){
        //match Gsrs Play API
        if(order ==null || order.trim().isEmpty()){
            return Sort.sort(getEntityClass());
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

    @GetGsrsRestApiMapping(value = {"/{id:$ID}", "({id:$ID})"})
    public ResponseEntity<Object> getById(@PathVariable String id, @RequestParam Map<String, String> queryParameters){
        Optional<T> obj = get(parseIdFromString(id));
        if(obj.isPresent()){
            return new ResponseEntity<>(obj.get(), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }
    @GetGsrsRestApiMapping(value = {"/{id:$NOT_ID}", "({id:$NOT_ID})"} )
    public ResponseEntity<Object> getByFlexId(@PathVariable String id, @RequestParam Map<String, String> queryParameters){
        Optional<T> obj = flexLookup(id);
        if(obj.isPresent()){
            return new ResponseEntity<>(obj.get(), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }
    @DeleteGsrsRestApiMapping(value = {"/{id:$ID}", "({id:$ID})"})
    public ResponseEntity<Object> deleteById(@PathVariable String id, @RequestParam Map<String, String> queryParameters){
        I parsedId = parseIdFromString(id);
        return deleteEntity(parsedId);
    }

    private ResponseEntity<Object> deleteEntity(I parsedId) {
        delete(parsedId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteGsrsRestApiMapping(value = {"/{id:$NOT_ID}", "({id:$NOT_ID})"} )
    public ResponseEntity<Object> deleteByFlexId(@PathVariable String id, @RequestParam Map<String, String> queryParameters){
        Optional<I> idOptional = flexLookupIdOnly(id);
        if(idOptional.isPresent()){

            return deleteEntity(idOptional.get());
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
