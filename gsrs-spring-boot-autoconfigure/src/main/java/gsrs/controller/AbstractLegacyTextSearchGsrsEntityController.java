package gsrs.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.DefaultDataSourceConfig;
import gsrs.cache.GsrsCache;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.controller.hateoas.HttpRequestHolder;
import gsrs.controller.hateoas.IxContext;
import gsrs.legacy.GsrsSuggestResult;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.repository.ETagRepository;
import gsrs.security.GsrsSecurityUtils;
import gsrs.security.hasAdminRole;
import gsrs.service.EtagExportGenerator;
import gsrs.services.TextService;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.EntityFetcher;
import ix.core.models.ETag;
import ix.core.search.GsrsLegacySearchController;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultContext;
import ix.core.search.bulk.UserSavedListService;
import ix.core.search.bulk.UserSavedListService.Operation;
import ix.core.search.bulk.BulkSearchService;
import ix.core.search.bulk.BulkSearchService.BulkQuerySummary;
import ix.core.search.bulk.ResultListRecord;
import ix.core.search.bulk.ResultListRecordGenerator;
import ix.core.search.text.FacetMeta;
import ix.core.search.text.TextIndexer;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import ix.utils.CallableUtil;
import ix.utils.Util;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Extension to AbstractGsrsEntityController that adds support for the legacy TextIndexer
 * and related search routes that would use it including {@code /@facets} , {@code /search} and {@code /search/@facets} .
 *
 * @param <T>
 * @param <I>
 */
@Slf4j
public abstract class AbstractLegacyTextSearchGsrsEntityController<C extends AbstractLegacyTextSearchGsrsEntityController, T, I> extends AbstractGsrsEntityController<C, T,I> implements GsrsLegacySearchController {

    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    private ETagRepository eTagRepository;
    
    @PersistenceContext(unitName =  DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
    private EntityManager localEntityManager;

    private final static ExecutorService executor = Executors.newFixedThreadPool(4);    
   
    @Data
    private class ReindexStatus{
    	private UUID statusID;
    	private String status;
    	private int total;
    	private int indexed;
    	private int failed;
    	private boolean done;
    	private long start;
    	private long finshed;
    	private List<String> ids;
    	private String _self;
    }
    
    @Data
    private class UserListStatus{
    	private UUID statusID;
    	private String status;
    	private int total;
    	private int processed;    	
    	private boolean done;    	   	
    }
    
    @Data
    private class ReindexStatusTasks{

    	public int getRunningCount() {
    		return (int) tasks.stream().filter(s->!s.isDone()).count();
    	}
    	public int getTotalCount() {
    		return (int) tasks.stream().filter(s->s.isDone()).count();
    	}
    	private List<ReindexStatus> tasks;
    	
    }
    
    @Autowired
    private TextService textService;
       
    @Autowired    
	protected GsrsCache gsrscache;
    
    @Autowired
    protected EntityLinks entityLinks;
    
    @Autowired
    protected UserSavedListService userSaveListService;
    
    
    //should maybe use cache
    //TODO: empty sometimes somehow
    private Map<String, ReindexStatus> reindexing = new ConcurrentHashMap<>();

    private final int BULK_SEARCH_DEFAULT_TOP = 1000;
    
    private final int BULK_SEARCH_DEFAULT_SKIP = 0;
    
    public ResultListRecordGenerator getResultListRecordGenerator() {
    	return new ResultListRecordGenerator (){};
    }
   
    /**
     * Force a reindex of all entities of this entity type.
     * @param wipeIndex should the whole index be deleted before re-index begins;
     *                  defaults to {@code false}.
     * @return
     */    
    @hasAdminRole
    @PostGsrsRestApiMapping(value="/@reindex", apiVersions = 1)
    public ResponseEntity forceFullReindex(@RequestParam(value= "wipeIndex", defaultValue = "false") boolean wipeIndex){
        getlegacyGsrsSearchService().reindexAndWait(wipeIndex);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @GetGsrsRestApiMapping(value="/@reindexBulk({id})", apiVersions = 1)
    public ResponseEntity bulkReindexStatus(@PathVariable("id") String id, @RequestParam Map<String, String> queryParameters,
    		HttpServletRequest request){
    	
    	return Optional.ofNullable(reindexing.get(id)).map(o->{
    		o.set_self(request.getRequestURL().toString());
    		return new ResponseEntity<>(o, HttpStatus.OK);	
    	})
    	.map(oo->(ResponseEntity)oo)
    	.orElseGet(()->{
    		return gsrsControllerConfiguration.handleNotFound(queryParameters);
    	});
    }
    
    @hasAdminRole
    @PostGsrsRestApiMapping(value="/@reindexBulk", apiVersions = 1)
    public ResponseEntity bulkReindex(@RequestBody String ids, @RequestParam Map<String, String> queryParameters){
    	List<String> queries = Arrays.asList(ids.split("\n"));
    	  
    	List<String> list = queries.stream()    			
    			.map(q->q.trim())
    			.peek(s->System.out.println(s))
    			.filter(q->q.length()>0)
    			.distinct()
    			.collect(Collectors.toList());  
    	ReindexStatus stat = new ReindexStatus();
    	stat.statusID = UUID.randomUUID();
    	stat.done=false;
    	stat.status="initializing";
    	stat.ids=list;
    	stat.start = TimeUtil.getCurrentTimeMillis();
    	stat.total=list.size();
    	reindexing.put(stat.statusID.toString(), stat);
    	
    	executor.execute(()->{
    		int[] r = new int[] {0};
    		stat.ids.forEach(id->{
    			r[0]++;
    			stat.setStatus("indexing record " + r[0] + " of "  + stat.total);
    			//TODO: Should change how this works probably to not use REST endpoint
    			try {
//    				Optional<T> obj = getEntityService().getEntityBySomeIdentifier(i);
//    				getlegacyGsrsSearchService().reindex(obj.get(), true);
//    				stat.indexed++;
   				
    				Optional<String> entityID = getEntityService().getEntityIdOnlyBySomeIdentifier(id).map(ii->ii.toString());
    				Class eclass = getEntityService().getEntityClass();
    				Key k = Key.ofStringId(eclass, entityID.get());
    				Object o = EntityFetcher.of(k).call();
        			getlegacyGsrsSearchService().reindex(o, true);

        			stat.indexed++;  			
    			}catch(Exception e) {
				log.warn("trouble reindexing id: " + id, e);
    				stat.failed++;
    			}   
    			
    		});
    		stat.setStatus("finished");
    		stat.done=true;
    		stat.finshed = TimeUtil.getCurrentTimeMillis();
    	});
    	
    	
        return new ResponseEntity<>(stat, HttpStatus.OK);
    }
    
    @GetGsrsRestApiMapping(value="/@reindexBulk", apiVersions = 1)
    public ResponseEntity bulkReindex(@RequestParam Map<String, String> queryParameters){
    	ReindexStatusTasks tasks = new ReindexStatusTasks();
    	tasks.tasks=reindexing.values().stream().collect(Collectors.toList());
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }
    @DeleteGsrsRestApiMapping(value="/@reindexBulk/clear", apiVersions = 1)
    public ResponseEntity bulkReindexClear(@RequestParam Map<String, String> queryParameters){
    	reindexing.entrySet().stream()
	    	.filter(e->e.getValue().isDone())
	    	.map(e->e.getKey())
	    	.collect(Collectors.toList())
	    	.forEach(k->reindexing.remove(k));
    	
    	ReindexStatusTasks tasks = new ReindexStatusTasks();
    	tasks.tasks=reindexing.values().stream().collect(Collectors.toList());
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }
    
    
    

    @PostGsrsRestApiMapping(value="({id})/@reindex", apiVersions = 1)
    public ResponseEntity reindex(@PathVariable("id") String id,
    		    		  		  @RequestParam Map<String, String> queryParameters){
        //this needs to trigger a reindex
        Optional<T> obj = getEntityService().getEntityBySomeIdentifier(id);
        if(obj.isPresent()){
            Key k = EntityWrapper.of(obj.get()).getKey();
            
            getlegacyGsrsSearchService().reindex(obj.get(), true /*delete first*/);
            //TODO: invent a better response?           
            return getIndexData(k.getIdString(),queryParameters);
        }    	
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }
    
    @GetGsrsRestApiMapping(value="({id})/@index", apiVersions = 1)
	public ResponseEntity getIndexData(@PathVariable("id") String id,
									   @RequestParam Map<String, String> queryParameters) {
		Optional<T> obj = getEntityService().getEntityBySomeIdentifier(id);
		if (obj.isPresent()) {
			Key k = EntityWrapper.of(obj.get()).getKey();
			try {
				TextIndexer.IndexRecord rec = getlegacyGsrsSearchService().getIndexData(k);
				return new ResponseEntity<>(rec, HttpStatus.OK);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return gsrsControllerConfiguration.handleNotFound(queryParameters);
	}

    @GetGsrsRestApiMapping(value = "/search/@facets", apiVersions = 1)
    public FacetMeta searchFacetFieldDrilldownV1(@RequestParam("q") Optional<String> query,
                                                 @RequestParam("field") Optional<String> field,
                                                 @RequestParam("top") Optional<Integer> top,
                                                 @RequestParam("skip") Optional<Integer> skip,
                                                 HttpServletRequest request) throws ParseException, IOException {
        SearchOptions so = new SearchOptions.Builder()
                .kind(getEntityService().getEntityClass())
                .top(Integer.MAX_VALUE) // match Play GSRS
                .fdim(10)
                .fskip(0)
                .ffilter("")
                .withParameters(request.getParameterMap())
                .build();
        so = this.instrumentSearchOptions(so); //add user

        TextIndexer.TermVectors tv= getlegacyGsrsSearchService().getTermVectorsFromQuery(query.orElse(null), so, field.orElse(null));
        return tv.getFacet(so.getFdim(), so.getFskip(), so.getFfilter(), StaticContextAccessor.getBean(IxContext.class).getEffectiveAdaptedURI(request).toString());


        //indexer.extractFullFacetQuery(this.query, this.options, field);
    }
    @GetGsrsRestApiMapping(value = "/@facets", apiVersions = 1)
    public FacetMeta searchFacetFieldV1(@RequestParam("field") Optional<String> field,
                                        @RequestParam("top") Optional<Integer> top,
                                        @RequestParam("skip") Optional<Integer> skip,
                                        HttpServletRequest request) throws ParseException, IOException {

        SearchOptions so = new SearchOptions.Builder()
                .fdim(10)
                .fskip(0)
                .ffilter("")
                .withParameters(Util.reduceParams(request.getParameterMap(),
                        "fdim", "fskip", "ffilter"))
                .build();
        
        so = this.instrumentSearchOptions(so);

        TextIndexer.TermVectors tv = getlegacyGsrsSearchService().getTermVectors(field);
        return tv.getFacet(so.getFdim(), so.getFskip(), so.getFfilter(), StaticContextAccessor.getBean(IxContext.class).getEffectiveAdaptedURI(request).toString());

    }

    /**
     * Get the implementation of {@link LegacyGsrsSearchService} for this entity type.
     * @return
     */
    protected abstract LegacyGsrsSearchService<T> getlegacyGsrsSearchService();

    /*
    GET     /suggest/@fields       ix.core.controllers.search.SearchFactory.suggestFields
GET     /suggest/:field       ix.core.controllers.search.SearchFactory.suggestField(field: String, q: String, max: Int ?= 10)
GET     /suggest       ix.core.controllers.search.SearchFactory.suggest(q: String, max: Int ?= 10)


     */
    @GetGsrsRestApiMapping("/suggest/@fields")
    public Collection<String> suggestFields() throws IOException {
        return getlegacyGsrsSearchService().getSuggestFields();
    }
    @GetGsrsRestApiMapping("/suggest")
    public Map<String, List<? extends GsrsSuggestResult>> suggest(@RequestParam(value ="q") String q, @RequestParam(value ="max", defaultValue = "10") int max) throws IOException {
        return getlegacyGsrsSearchService().suggest(q, max);
    }
    @GetGsrsRestApiMapping("/suggest/{field}")
    public List<? extends GsrsSuggestResult> suggestField(@PathVariable("field") String field,  @RequestParam("q") String q, @RequestParam(value ="max", defaultValue = "10") int max) throws IOException {
        return getlegacyGsrsSearchService().suggestField(field, q, max);
    }
    
    @GetGsrsRestApiMapping(value = "/search", apiVersions = 1)
    public ResponseEntity<Object> searchV1(@RequestParam("q") Optional<String> query,
                                           @RequestParam("top") Optional<Integer> top,
                                           @RequestParam("skip") Optional<Integer> skip,
                                           @RequestParam("fdim") Optional<Integer> fdim,
                                           HttpServletRequest request,
                                           @RequestParam Map<String, String> queryParameters){
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .query(query.orElse(null))
                .kind(getEntityService().getEntityClass());

        top.ifPresent( t-> builder.top(t));
        skip.ifPresent( t-> builder.skip(t));
        fdim.ifPresent( t-> builder.fdim(t));

        SearchRequest searchRequest = builder.withParameters(request.getParameterMap())
                .build();

        this.instrumentSearchRequest(searchRequest);
        
        SearchResult result = null;
        try {
            result = getlegacyGsrsSearchService().search(searchRequest.getQuery(), searchRequest.getOptions() );
        } catch (Exception e) {
            return getGsrsControllerConfiguration().handleError(e, queryParameters);
        }
        
        SearchResult fresult=result;
        
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(true);        
        List results = (List) transactionTemplate.execute(stauts -> {
            //the top and skip settings  look wrong, because we're not skipping
            //anything, but it's actually right,
            //because the original request did the skipping.
            //This mechanism should probably be worked out
            //better, as it's not consistent.

            //Note that the SearchResult uses a LazyList,
            //but this is copying to a real list, this will
            //trigger direct fetches from the lazylist.
            //With proper caching there should be no further
            //triggered fetching after this.

            String viewType=queryParameters.get("view");
            if("key".equals(viewType)){
                List<ix.core.util.EntityUtils.Key> klist=new ArrayList<>(Math.min(fresult.getCount(),1000));
                fresult.copyKeysTo(klist, 0, top.orElse(10), true); 
                return klist;
            }else{
                List tlist = new ArrayList<>(top.orElse(10));
                fresult.copyTo(tlist, 0, top.orElse(10), true);
                return tlist;
            }
        });

        
        //even if list is empty we want to return an empty list not a 404
        ResponseEntity<Object> ret= new ResponseEntity<>(createSearchResponse(results, result, request), HttpStatus.OK);
        return ret;
    }
//
//    protected abstract Object createSearchResponse(List<Object> results, SearchResult result, HttpServletRequest request);


    @PostGsrsRestApiMapping(value="/@bulkQuery")
    public ResponseEntity<String> saveQueryList(@RequestBody String query,
    									@RequestParam("top") Optional<Integer> top,
  										@RequestParam("skip") Optional<Integer> skip,
  										HttpServletRequest request){
    	
    	int qTop = BULK_SEARCH_DEFAULT_TOP, qSkip = BULK_SEARCH_DEFAULT_SKIP;
    	if(top.isPresent()) 
    		qTop = top.get();
    	if(skip.isPresent()) 
    		qSkip = skip.get();
    	    	
    	List<String> queries = Arrays.asList(query.split("\\r?\\n|\\r"));
  	  
    	List<String> list = queries.stream()    			
    			.map(q->q.trim())
    			.filter(q->q.length()>0)
//    			.distinct()                            No need to be distinct
    			.collect(Collectors.toList());    	
    	
    	String queryStringToSave = list.stream().collect(Collectors.joining("\n"));
    	Long id = textService.saveTextString("bulkSearch", queryStringToSave);
    	
    	String uri = request.getRequestURL().toString(); 
    	if(request.getQueryString()!=null)
    		uri = uri + "?"+request.getQueryString();
    	else
    		uri = uri + "?top=" + qTop + "&skip=" + qSkip; 
    	    	
    	String returnJsonSrting = createJson(id, qTop, qSkip, list, uri);    	
 
        return new ResponseEntity<>(returnJsonSrting, HttpStatus.OK);
    }
    
    
    @PutGsrsRestApiMapping(value="/@bulkQuery")
    public ResponseEntity<String> updateQueryList(@RequestBody String query,
    									@RequestParam("id") String queryId,
    									@RequestParam("top") Optional<Integer> top,
  										@RequestParam("skip") Optional<Integer> skip,
  										HttpServletRequest request){
    	
    	Long id = Long.parseLong(queryId);
    	if(id < 0) {
    		return new ResponseEntity<>("Invalid ID " + id, HttpStatus.BAD_REQUEST);    		
    	}
    	
    	int qTop = BULK_SEARCH_DEFAULT_TOP, qSkip = BULK_SEARCH_DEFAULT_SKIP;
    	if(top.isPresent()) 
    		qTop = top.get();
    	if(skip.isPresent()) 
    		qSkip = skip.get();
    	    	
    	List<String> queries = Arrays.asList(query.split("\\r?\\n|\\r"));
  	  
    	List<String> list = queries.stream()    			
    			.map(q->q.trim())
    			.filter(q->q.length()>0)
//    			.distinct()                            No need to be distinct
    			.collect(Collectors.toList());    	
    	
    	String queryStringToSave = list.stream().collect(Collectors.joining("\n"));    	
    	
    	Long returnId = textService.updateTextString("bulkSearch", queryId, queryStringToSave);
    	
    	String uri = request.getRequestURL().toString(); 
    	if(request.getQueryString()!=null)
    		uri = uri + "?"+request.getQueryString();
    	else
    		uri = uri + "?top=" + qTop + "&skip=" + qSkip; 
    	    	
    	String returnJsonSrting = createJson(returnId, qTop, qSkip, list, uri);    	
 
        return new ResponseEntity<>(returnJsonSrting, HttpStatus.OK);
    }


    @GetGsrsRestApiMapping(value="/@bulkQuery")
    public ResponseEntity<String> getQueryList(@RequestParam String id,
    										   @RequestParam("top") Optional<Integer> top,
    										   @RequestParam("skip") Optional<Integer> skip,
    										   HttpServletRequest request){    	
    	
    	String queryString = textService.getText(id); 
    	int qTop = BULK_SEARCH_DEFAULT_TOP, qSkip = BULK_SEARCH_DEFAULT_SKIP;
    	if(top.isPresent()) 
    		qTop = top.get();
    	if(skip.isPresent()) 
    		qSkip = skip.get();
    	List<String> queries = Arrays.asList(queryString.split("\n"));
    	List<String> list = queries.stream()
    								.map(p->p.trim())
    								.collect(Collectors.toList());  
    	
    	String uri = request.getRequestURL().toString(); 
    	if(request.getQueryString()!=null)
    		uri = uri + "?" + request.getQueryString();
    	else
    		uri = uri + "?top=" + qTop + "&skip=" + qSkip; 
    	
    	String returnJson = createJson(Long.parseLong(id), qTop, qSkip, list, uri);
        return new ResponseEntity<>(returnJson, HttpStatus.OK);
    }
    
    @DeleteGsrsRestApiMapping(value="/@bulkQuery")
    public ResponseEntity<String> deleteQueryList(@RequestParam String id){    	
    	textService.deleteText(id); 	    	
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
	@GetGsrsRestApiMapping(value = "/bulkSearch", apiVersions = 1)
	public ResponseEntity<Object> bulkSearch(@RequestParam("bulkQID") String queryListID,
			@RequestParam("q") Optional<String> query, @RequestParam("top") Optional<Integer> top,
			@RequestParam("skip") Optional<Integer> skip, @RequestParam("qTop") Optional<Integer> qTop,
			@RequestParam("qSkip") Optional<Integer> qSkip,@RequestParam("fdim") Optional<Integer> fdim,
			@RequestParam("searchOnIdentifiers") Optional<Boolean> searchOnIdentifiers,
			HttpServletRequest request, @RequestParam Map<String, String> queryParameters) {
		SearchRequest.Builder builder = new SearchRequest.Builder().query(query.orElse(null))
				.kind(getEntityService().getEntityClass());

		top.ifPresent(t -> builder.top(t));
		skip.ifPresent(t -> builder.skip(t));
		fdim.ifPresent(t -> builder.fdim(t));
		qTop.ifPresent(t -> builder.qTop(t));
		qSkip.ifPresent(t -> builder.qSkip(t));
		
		searchOnIdentifiers.ifPresent(t -> builder.bulkSearchOnIdentifiers(t.booleanValue()));
				
		SearchRequest searchRequest = builder.withParameters(request.getParameterMap()).build();
		searchRequest = this.instrumentSearchRequest(searchRequest);

		BulkSearchService.SanitizedBulkSearchRequest sanitizedRequest = new BulkSearchService.SanitizedBulkSearchRequest();

		List<String> queries = new ArrayList<>();
		try {
			queries = gsrscache.getOrElse("/BulkID/" + queryListID, () -> {

				String queryString = textService.getText(queryListID);
				if (queryString.isEmpty()) {
					throw new RuntimeException("Cannot find bulk query ID. ");
				}
				return Arrays.asList(queryString.split("\n"));

			});
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return new ResponseEntity<>(e1.getMessage(), HttpStatus.NOT_FOUND);
		}

		sanitizedRequest.setQueries(queries);
		SearchOptions searchOptions = searchRequest.getOptions();
		SearchResultContext resultContext;
		try {
			resultContext = getlegacyGsrsSearchService().bulkSearch(sanitizedRequest, searchOptions);
			updateSearchContextGenerator(resultContext, queryParameters);

			// TODO: need to add support for qText in the "focused" version of
			// all structure searches. This may require some deeper changes.
			SearchResultContext focused = resultContext.getFocused(SearchOptions.DEFAULT_TOP, 0,
					searchOptions.getFdim(), "");
			if(resultContext.getKey() != null)
				focused.setKey(resultContext.getKey());
			return entityFactoryDetailedSearch(focused, false);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new ResponseEntity<>("Error during bulk search!", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
    
    
    private String createJson(Long id, int top, int skip, List<String> queries, String uri){
    	
    	List<String> sublist = new ArrayList<String>();
    	int endIndex = Math.min(top+skip,queries.size());    		
    	if(skip < queries.size())
    		sublist = queries.subList(skip, endIndex);
    	ObjectMapper mapper = new ObjectMapper();
    	ObjectNode baseNode = mapper.createObjectNode();   	   	
    	
    	baseNode.put("id", id);
    	baseNode.put("total", queries.size());
    	baseNode.put("count", sublist.size());
    	baseNode.put("top", top);
    	baseNode.put("skip", skip);    	
    	ArrayNode listNode = baseNode.putArray("queries");
    	sublist.forEach(listNode::add);    	   	
    	baseNode.put("_self", uri);
    	
    	return baseNode.toPrettyString();
    }
        
    protected abstract Object createSearchResponse(List<Object> results, SearchResult result, HttpServletRequest request);

    static String getOrderedKey (SearchResultContext context, SearchRequest request) {
        return "fetchResult/"+context.getId() + "/" + request.getOrderedSetSha1();
    }
    static String getKey (SearchResultContext context, SearchRequest request) {
        return "fetchResult/"+context.getId() + "/" + request.getDefiningSetSha1();
    }
    
    public SearchResult getResultFor(SearchResultContext ctx, SearchRequest req, boolean preserveOrder)
            throws IOException, Exception{

        final String key = (preserveOrder)? getOrderedKey(ctx,req):getKey (ctx, req);

        CallableUtil.TypedCallable<SearchResult> tc = CallableUtil.TypedCallable.of(() -> {
            Collection results = ctx.getResults();
            SearchRequest request = new SearchRequest.Builder()
                    .subset(results)
                    .options(req.getOptions())
                    .skip(0)
                    .top(results.size())
                    .query(req.getQuery())
                    .build();
            request=instrumentSearchRequest(request);

            SearchResult searchResult =null;

            if (results.isEmpty()) {
                searchResult= SearchResult.createEmptyBuilder(req.getOptions())
                        .build();
            }else{
                //katzelda : run it through the text indexer for the facets?
//                searchResult = SearchFactory.search (request);            	
                searchResult = getlegacyGsrsSearchService().search(request.getQuery(), request.getOptions(), request.getSubset());
                log.debug("Cache misses: "
                        +key+" size="+results.size()
                        +" class="+searchResult);
            }

            // make an alias for the context.id to this search
            // result
            searchResult.setKey(ctx.getId());
            return searchResult;
        }, SearchResult.class);

        if(ctx.isDetermined()) {
            return gsrscache.getOrElse(key, tc);
        }else {
            return tc.call();
        }
    }

    public ResponseEntity<Object> entityFactoryDetailedSearch(SearchResultContext context, boolean sync) throws InterruptedException, ExecutionException {
        context.setAdapter((srequest, ctx) -> {
            try {
                // TODO: technically this shouldn't be needed,
                // but something is getting lost in translation between 2.X and 3.0
                // and it's leading to some results coming back which are not substances.
                // This is particularly strange since there is an explicit subset which IS
                // all substances given.
            	
                srequest.getOptions().setKind(this.getEntityService().getEntityClass());
                SearchResult sr = getResultFor(ctx, srequest,true);
                
                if(ctx.getKey() != null) {                	
                	BulkQuerySummary summary = (BulkQuerySummary)gsrscache.getRaw("BulkSearchSummary/"+ctx.getKey());
                	if(summary!= null)
                		sr.setSummary(summary);                	
                }

                List<T> rlist = new ArrayList<>();

                sr.copyTo(rlist, srequest.getOptions().getSkip(), srequest.getOptions().getTop(), true); // synchronous
//                for (T s : rlist) { 
//                	if(s instanceof BaseModel) {
//                		((BaseModel)s).setMatchContextProperty(gsrscache.getMatchingContextByContextID(ctx.getId(), EntityUtils.EntityWrapper.of(s).getKey().toRootKey()));
//                	}
//                }
                return sr;
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException("Error fetching search result", e);
            }
        });


        if (sync) {
            try {
                context.getDeterminedFuture().get(1, TimeUnit.MINUTES);
                HttpHeaders headers = new HttpHeaders();

                //TODO this should actually forward to "status(<key>)/results", but it's currently status/<key>/results
                headers.add("Location", GsrsLinkUtil.adapt(context.getKey(),entityLinks.linkFor(SearchResultContext.class).slash(context.getKey()).slash("results").withSelfRel())
                        .toUri().toString() );
                return new ResponseEntity<>(headers,HttpStatus.FOUND);
            } catch (TimeoutException e) {
                log.warn("Structure search timed out!", e);
            }
        }
        return new ResponseEntity<>(context, HttpStatus.OK);
    }

    protected void updateSearchContextGenerator(SearchResultContext resultContext, Map<String,String> queryParameters) {
        String oldURL = resultContext.getGeneratingUrl();
        if(oldURL!=null && !oldURL.contains("?")) {
            //we have to manually set the actual request uri here as it's the only place we know it!!
            //for some reason the spring boot methods to get the current quest  URI don't include the parameters
            //so we have to append them manually here from our controller
            StringBuilder queryParamBuilder = new StringBuilder();
            queryParameters.forEach((k,v)->{
                if(queryParamBuilder.length()==0){
                    queryParamBuilder.append("?");
                }else{
                    queryParamBuilder.append("&");
                }
                queryParamBuilder.append(k).append("=").append(v);
            });
            resultContext.setGeneratingUrl(oldURL + queryParamBuilder);            
        }
    }
    
    
    @GetGsrsRestApiMapping(value="/@userLists/currentUser")
    public ResponseEntity<String> getCurrentUserSavedLists(
    										   @RequestParam("top") Optional<Integer> top,
    										   @RequestParam("skip") Optional<Integer> skip){
    	
    	if(!GsrsSecurityUtils.getCurrentUsername().isPresent())
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND); 
    	
    	String name = GsrsSecurityUtils.getCurrentUsername().get();
    	    	
    	int rTop = top.orElse(BULK_SEARCH_DEFAULT_TOP);   
    	int rSkip = skip.orElse(BULK_SEARCH_DEFAULT_SKIP);
    	List<String> list = userSaveListService.getUserSearchResultLists(name);
    	
    	return new ResponseEntity<>(getBulkSearchResultListNamesString(rTop, rSkip, list), HttpStatus.OK);   	
    	
    }
    
    @hasAdminRole
    @GetGsrsRestApiMapping(value="/@userLists/otherUser")
    public ResponseEntity<String> getOtherUserSavedLists(@RequestParam("name") Optional<String> name,
    										   @RequestParam("top") Optional<Integer> top,
    										   @RequestParam("skip") Optional<Integer> skip){
    	
    	List<String> list; 
    	if(!name.isPresent())
    		list = userSaveListService.getAllUserSearchResultLists();
    	else 
    		list = userSaveListService.getUserSearchResultLists(name.get());
    	
    	int rTop = top.orElse(BULK_SEARCH_DEFAULT_TOP);   
    	int rSkip = skip.orElse(BULK_SEARCH_DEFAULT_SKIP);    	    	
    	return new ResponseEntity<>(getBulkSearchResultListNamesString(rTop, rSkip, list), HttpStatus.OK); 		
    }
    
    private String getBulkSearchResultListNamesString(int top, int skip, List<String> list) {
    	List<String> topList;
    	if(list.size() <= top)
    		topList = list;
    	else
    		topList = list.subList(0, top);
    	
    	ObjectMapper mapper = new ObjectMapper();
    	ObjectNode baseNode = mapper.createObjectNode();   	   	
    	    	
    	baseNode.put("top", top);
    	baseNode.put("skip", skip);    	
    	ArrayNode listNode = baseNode.putArray("lists");
    	topList.forEach(listNode::add);   
    	
    	return baseNode.toPrettyString();
    }
    
    private String getBulkSearchResultListContentString(int top, int skip, List<String> list) {
    	List<String> topList;
    	if(list.size() <= top)
    		topList = list;
    	else
    		topList = list.subList(0, top);
    	    	
    	ObjectMapper mapper = new ObjectMapper();
    	ObjectNode baseNode = mapper.createObjectNode();   	   	
    	    	
    	baseNode.put("top", top);
    	baseNode.put("skip", skip);    	
    	ArrayNode listNode = baseNode.putArray("lists");
    	
    	ResultListRecordGenerator generator = getResultListRecordGenerator();
    	    	
    	for(String key: topList) {
    		ResultListRecord record = generator.generate(key);  
    		
    		ObjectNode node = mapper.createObjectNode();
    		node.put("key", key);
    		String displayName = record.getDisplayName();
    		String displayCode = record.getDisplayCode();
    		String displayCodeSystem = record.getDisplayCodeSystem();
			if(displayName!=null && !displayName.isEmpty()) {
				node.put("displayName", displayName);
			}else {
				node.put("displayName", "");
			}
			if(displayCode!=null && !displayCode.isEmpty()) {
				node.put("displayCode", displayCode);
			}else {
				node.put("displayCode", "");
			}
			if(displayCodeSystem!=null && !displayCodeSystem.isEmpty()) {
				node.put("displayCodeSystem", displayCodeSystem);
			}else {
				node.put("displayCodeSystem", "");
			}
			
			listNode.add(node);   
    	
    	}
    	
    	return baseNode.toPrettyString();
    }
    
    @GetGsrsRestApiMapping(value="/@userList/{list}")
    public ResponseEntity<String> getCurrentUserSavedListContent(@PathVariable String list,
    										   @RequestParam("top") Optional<Integer> top,
    										   @RequestParam("skip") Optional<Integer> skip){
    	
    	if(!GsrsSecurityUtils.getCurrentUsername().isPresent())
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND); 
    	    	
    	String userName = GsrsSecurityUtils.getCurrentUsername().get();
    	    	    	
    	int rTop = top.orElse(BULK_SEARCH_DEFAULT_TOP);   
    	int rSkip = skip.orElse(BULK_SEARCH_DEFAULT_SKIP);
    	List<String> keys = userSaveListService.getUserSavedBulkSearchResultListContent(userName, list, rTop, rSkip);
    	    	
    	return new ResponseEntity<>(getBulkSearchResultListContentString(rTop, rSkip, keys), HttpStatus.OK);  	
    	
    }
    
    @hasAdminRole
    @GetGsrsRestApiMapping(value="/@userList/{user}/{list}")
    public ResponseEntity<String> getOtherUserSavedListContent(@PathVariable String name,
    										   @PathVariable String list,
    										   @RequestParam("top") Optional<Integer> top,
    										   @RequestParam("skip") Optional<Integer> skip){
    	
  	    	
    	String userName = name;    	
     	
    	int rTop = top.orElse(BULK_SEARCH_DEFAULT_TOP);   
    	int rSkip = skip.orElse(BULK_SEARCH_DEFAULT_SKIP);
    	List<String> keys = userSaveListService.getUserSavedBulkSearchResultListContent(userName, list, rTop, rSkip);
    	   	
    	return new ResponseEntity<>(getBulkSearchResultListContentString(rTop, rSkip, keys), HttpStatus.OK);  	
    	
    }
    
    private UserListStatus createUserListStatus() {
    	UserListStatus listStatus = new UserListStatus();
    	listStatus.statusID = UUID.randomUUID();
    	listStatus.done=false;
    	listStatus.status="Initializing...";    	 
    	listStatus.processed = 0;
    	return listStatus;
    }
    
    // if the user list exists, it will fail
    @PostGsrsRestApiMapping(value="/@userList/keys")  
    public ResponseEntity<String> createUserListWithKeys(  											
    										   @RequestParam String listName,
    										   @RequestBody String keys){ 
    	
    	
    	if(!GsrsSecurityUtils.getCurrentUsername().isPresent())
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND); 
    	
    	String userName = GsrsSecurityUtils.getCurrentUsername().get();
    	   	
    	if(!validStringParamater(listName) || !validStringParamater(keys)) {
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);    	} 
    	   	
    	
    	List<String> keyIdList = Arrays.asList(keys.split(","));
    	    	
    	List<String> list = keyIdList.stream().map(key->key.trim())
    										.filter(key->!key.isEmpty())
    										.collect(Collectors.toList());
    	
    	if(list.size() ==0)
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);    	
    	
    	UserListStatus status = createUserListStatus(); 
    	status.total=list.size();
    	
    	String message = userSaveListService.validateUsernameAndListname(userName, listName);
    	if(message.length()>0)
    		return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST); 
    	
    	executor.execute(()->{      
    		userSaveListService.createBulkSearchResultList(userName, listName, list);
    		reIndexWithKeys(status,list);    		
    	});
    	
    	log.warn("testing ");
    	return new ResponseEntity<>(generateResultIDJson(status.statusID.toString()), HttpStatus.OK);	
    }
    //api/v1/substance/@userList/7c9f73c931335ca3?listName="myList"
    @PostGsrsRestApiMapping(value="/@userList/etag/{etagId}")  //change to user list
    public ResponseEntity<String> createUserListWithEtag(  											
    										   @RequestParam(value="listName",required=true) String listName,
    										   @PathVariable("etagId") String etagId,
    										   HttpServletRequest request){ //take an etag and get all the keys
    	
    	
    	log.info("in saveUserListWithEtag");
    	Optional<ETag> etagObj = eTagRepository.findByEtag(etagId);
    	if(!etagObj.isPresent()) {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    	EtagExportGenerator<T> etagExporter = new EtagExportGenerator<T>(localEntityManager, transactionManager, HttpRequestHolder.fromRequest(request));
    	List<Key> keys = etagExporter.getKeysFromUri(etagObj.get(), getEntityService().getContext());
    	
    	if(!GsrsSecurityUtils.getCurrentUsername().isPresent())
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND); 
    	
    	String userName = GsrsSecurityUtils.getCurrentUsername().get();
    	   	
    	if(!validStringParamater(listName) ) {
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);    	} 
    	   	
    	List<String> keyList = keys.stream()
    			.map(key->key.getIdString())
    			.map(key->key.trim())
    			.filter(key->!key.isEmpty())
    			.collect(Collectors.toList());
   	
    	if(keyList.size() ==0)
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    	
    	String message = userSaveListService.validateUsernameAndListname(userName, listName);
    	if(message.length()>0)
    		return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);     	
    	
    	UserListStatus listStatus = createUserListStatus();  
    	listStatus.total=keyList.size();
    	
    	executor.execute(()->{   
    		userSaveListService.createBulkSearchResultList(userName, listName, keyList);       	
    		reIndexWithKeys(listStatus,keyList);
    	});    	

    	return new ResponseEntity<>(generateResultIDJson(listStatus.statusID.toString()), HttpStatus.OK);	
    }
        
    
    
    @DeleteGsrsRestApiMapping(value="/@userList/currentUser")
    public ResponseEntity<String> deleteCurrentUserSavedList(   											
    										   @RequestParam String listName,    										   
    										   HttpServletRequest request){ 
    	if(!validStringParamater(listName)) {
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    	} 
    	    	
    	if(!GsrsSecurityUtils.getCurrentUsername().isPresent())
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);  
    	
    	String userName = GsrsSecurityUtils.getCurrentUsername().get();
    	
    	List<String> keys = userSaveListService.getUserSavedBulkSearchResultListContent(userName, listName);
    	
    	UserListStatus listStatus = createUserListStatus();  
    	listStatus.total=keys.size();
    	
    	executor.execute(()->{  
    		userSaveListService.deleteBulkSearchResultList(userName, listName);    	
    		reIndexWithKeys(listStatus,keys);
    	});
    	
    	
    	return new ResponseEntity<>(generateResultIDJson(listStatus.statusID.toString()), HttpStatus.OK);	
    }
    
    @hasAdminRole
    @DeleteGsrsRestApiMapping(value="/@userList/otherUser")
    public ResponseEntity<String> deleteOtherUserSavedList(@RequestParam String userName,    											
    										   @RequestParam String listName,    										   
    										   HttpServletRequest request){ 
    	if(!validStringParamater(userName) || !validStringParamater(listName)) {
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    	} 
    	
    	List<String> keys = userSaveListService.getUserSavedBulkSearchResultListContent(userName, listName);
    	
    	UserListStatus listStatus = createUserListStatus();  
    	listStatus.total=keys.size();
    	
    	executor.execute(()->{  
    		userSaveListService.deleteBulkSearchResultList(userName, listName);  	
    		reIndexWithKeys(listStatus,keys);
    	});    	
    	
    	return new ResponseEntity<>(generateResultIDJson(listStatus.statusID.toString()), HttpStatus.OK);	
    }
    
    
    @PutGsrsRestApiMapping(value="/@userList/currentUser/etag/{etagId}") 
    public ResponseEntity<Object> addToCurrentUserSavedListWithEtag( 
    		@RequestParam(value="listName",required=true) String listName,
			   @PathVariable("etagId") String etagId,
			   HttpServletRequest request){
    	
    	Optional<ETag> etagObj = eTagRepository.findByEtag(etagId);
    	if(!etagObj.isPresent()) {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    	EtagExportGenerator<T> etagExporter = new EtagExportGenerator<T>(localEntityManager, transactionManager, HttpRequestHolder.fromRequest(request));
    	List<Key> keys = etagExporter.getKeysFromUri(etagObj.get(), getEntityService().getContext());
    	
    	if(!GsrsSecurityUtils.getCurrentUsername().isPresent())
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND); 
    	
    	String userName = GsrsSecurityUtils.getCurrentUsername().get();    	
    	
    	if(!validStringParamater(listName) ) {
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);}
    	
    	boolean listExists = userSaveListService.userListExists(userName,listName);
    	if(!listExists) {
    		return new ResponseEntity<Object>(
    				GsrsControllerConfiguration.createStatusJson("List does not exist!", HttpStatus.NOT_FOUND.value()), 
    				HttpStatus.NOT_FOUND );
    	}
    	    	   	
    	List<String> keyList = keys.stream()
    			.map(key->key.getIdString())
    			.map(key->key.trim())
    			.filter(key->!key.isEmpty())
    			.collect(Collectors.toList());
   	
    	if(keyList.size() ==0)
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    	
    	boolean updated = userSaveListService.updateBulkSearchResultList(userName, listName, keyList, Operation.ADD);
    	if(updated) {
    		UserListStatus listStatus = createUserListStatus();  
        	listStatus.total=keyList.size();
    		executor.execute(()->{   			  	
    			reIndexWithKeys(listStatus,keyList);    			
    		});    		
    		return new ResponseEntity<>(generateResultIDJson(listStatus.statusID.toString()), HttpStatus.OK);	
    	}else {
    		return new ResponseEntity<>(HttpStatus.OK);    		
    	}    	
    }   			
    
    
    @PutGsrsRestApiMapping(value="/@userList/currentUser") 
    public ResponseEntity<String> updateCurrentUserSavedList(   											
    										   @RequestParam String listName,
    										   @RequestBody String keys,
    										   @RequestParam String operation,
    										   HttpServletRequest request){
    	
    	if(!validStringParamater(listName) ||	!validStringParamater(keys) ) {
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    	}    	
    	
    	UserSavedListService.Operation op = UserSavedListService.Operation.valueOf(operation.trim().toUpperCase());
    	if(op == null) {
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    	}
    	
    	if(!GsrsSecurityUtils.getCurrentUsername().isPresent())
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);  
    	
    	String userName = GsrsSecurityUtils.getCurrentUsername().get();
    	
    	boolean listExists = userSaveListService.userListExists(userName,listName);
    	if(!listExists) {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    	    	
    	List<String> keyList = Arrays.asList(keys.split(","));
    	
    	List<String> list = keyList.stream().map(key->key.trim())
    										.filter(key->!key.isEmpty())
    										.collect(Collectors.toList());
    	
    	if(list.size() ==0)
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    	    	
    	boolean updated = userSaveListService.updateBulkSearchResultList(userName, listName, list, op);
    	if(updated) {
    		UserListStatus listStatus = createUserListStatus();  
        	listStatus.total=list.size();
    		executor.execute(()->{   			  	
    			reIndexWithKeys(listStatus,list);    			
    		});    		
    		return new ResponseEntity<>(generateResultIDJson(listStatus.statusID.toString()), HttpStatus.OK);	
    	}else {
    		return new ResponseEntity<>(HttpStatus.OK);    		
    	}    		
    }
    
    @hasAdminRole
    @PutGsrsRestApiMapping(value="/@userList/otherUser")
    public ResponseEntity<String> updateOtherUserSavedList(   		
    										   @RequestParam String userName, 	
    										   @RequestParam String listName,
    										   @RequestBody String keys,
    										   @RequestParam String operation,
    										   HttpServletRequest request){
    	
    	if(!validStringParamater(listName) || !validStringParamater(userName)||	!validStringParamater(keys) ) {
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    	}    	
    	
    	UserSavedListService.Operation op = UserSavedListService.Operation.valueOf(operation.trim().toUpperCase());
    	if(op == null) {
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    	}
    	
    	boolean listExists = userSaveListService.userListExists(userName,listName);
    	if(!listExists) {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    	
    	List<String> keyList = Arrays.asList(keys.split(","));
    	
    	List<String> list = keyList.stream().map(key->key.trim())
    										.filter(key->!key.isEmpty())
    										.collect(Collectors.toList());
    	
    	if(list.size() ==0)
    		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    	
    	boolean updated = userSaveListService.updateBulkSearchResultList(userName, listName, list, op);
    	
    	if(updated) {
    		UserListStatus listStatus = createUserListStatus();  
    		listStatus.total=list.size();
    		executor.execute(()->{   			  	
    			reIndexWithKeys(listStatus,list);    			
    		});    		
    		return new ResponseEntity<>(generateResultIDJson(listStatus.statusID.toString()), HttpStatus.OK);	
    	}else {
    		return new ResponseEntity<>(HttpStatus.OK);    		
    	}    	
    }
    
    @GetGsrsRestApiMapping(value="/@userList/status/{id}")    
    public ResponseEntity<String> getSaveUserListStatus(@PathVariable("id") String id){
    	    	
    	UserListStatus status = (UserListStatus)gsrscache.getRaw("UserSavedList/" + id);
    	if(status ==null){
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    	ObjectMapper mapper = new ObjectMapper();
    	ObjectNode node = mapper.createObjectNode();   	
    	node.put("id", id);
    	node.put("status", status.getStatus());    	
    	return new ResponseEntity<>(node.toPrettyString(), HttpStatus.OK);
    }
    
    boolean validStringParamater(String param) {
    	if(param == null)
    		return false;
    	else if(param.trim().isEmpty())
    		return false;
    	return true;
    }
    
    private void reIndexWithKeys(UserListStatus status, List<String> keyIds) { 
    	
    	int total = status.getTotal();
    	for(String id: keyIds) {    		
    		try {

    			Optional<String> entityID = getEntityService().getEntityIdOnlyBySomeIdentifier(id).map(ii->ii.toString());
    			
    			if(entityID.isPresent()) {    			
    				Class eclass = getEntityService().getEntityClass();
                    Key k = Key.ofStringId(eclass, entityID.get());
                    Object o = EntityFetcher.of(k).call();                    
    				getlegacyGsrsSearchService().reindex(o, true);    				
    			
    			}else {
    				log.warn("Cannot get the object during reindexing id: " + id);
    			}    			
    			status.processed ++;    			
    			if(status.processed < total) {
    				status.status = "Processing " + status.processed + " of " + status.total + ".";
    			}else {
    				status.status = "Completed.";
    				status.done = true;
    			}
    			
    			gsrscache.setRaw("UserSavedList/" + status.getStatusID(), status);
			
    		}catch(Exception e) {
    			log.warn("trouble reindexing id: " + id, e);
			
    		}   
    	}
		
    	
    }
    
    private String generateResultIDJson(String id) {
    	ObjectMapper mapper = new ObjectMapper();
    	ObjectNode node = mapper.createObjectNode();   	
    	node.put("id", id);
    	return node.toPrettyString();    	
    }    
}
