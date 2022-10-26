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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.controller.hateoas.IxContext;
import gsrs.legacy.GsrsSuggestResult;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.security.hasAdminRole;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.search.GsrsLegacySearchController;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.FacetMeta;
import ix.core.search.text.TextIndexer;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import ix.utils.Util;
import lombok.Data;

/**
 * Extension to AbstractGsrsEntityController that adds support for the legacy TextIndexer
 * and related search routes that would use it including {@code /@facets} , {@code /search} and {@code /search/@facets} .
 *
 * @param <T>
 * @param <I>
 */
public abstract class AbstractLegacyTextSearchGsrsEntityController<C extends AbstractLegacyTextSearchGsrsEntityController, T, I> extends AbstractGsrsEntityController<C, T,I> implements GsrsLegacySearchController {

//    public AbstractLegacyTextSearchGsrsEntityController(String context, IdHelper idHelper) {
//        super(context, idHelper);
//    }
//    public AbstractLegacyTextSearchGsrsEntityController(String context, Pattern idPattern) {
//        super(context, idPattern);
//    }
    @Autowired
    private PlatformTransactionManager transactionManager;

    private final static ExecutorService executor = Executors.newFixedThreadPool(1);
    
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
    	//TODO need self
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
    
    //should maybe use cache
    //TODO: empty sometimes somehow
    private Map<String, ReindexStatus> reindexing = new ConcurrentHashMap<>();


    
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
    public ResponseEntity bulkReindexStatus(@PathVariable("id") String id, @RequestParam Map<String, String> queryParameters){
    	
    	return Optional.ofNullable(reindexing.get(id)).map(o->{
    		return new ResponseEntity<>(o, HttpStatus.OK);	
    	})
    	.map(oo->(ResponseEntity)oo)
    	.orElseGet(()->{
    		return gsrsControllerConfiguration.handleNotFound(queryParameters);
    	});
    }
//    @hasAdminRole
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
    		stat.ids.forEach(i->{
    			r[0]++;
    			stat.setStatus("indexing record " + r[0] + " of "  + stat.total);
    			//TODO: Should change how this works probably to not use REST endpoint
    			try {
    				Optional<T> obj = getEntityService().getEntityBySomeIdentifier(i);
    				getlegacyGsrsSearchService().reindex(obj.get(), true);
    				stat.indexed++;
    			}catch(Exception e) {
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
    @GetGsrsRestApiMapping(value="/@reindexBulk/clear", apiVersions = 1)
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
    
    
    
//    @hasAdminRole
    @PostGsrsRestApiMapping(value="({id})/@reindex", apiVersions = 1)
    public ResponseEntity reindex(@PathVariable("id") String id,
    		    		  		  @RequestParam(value = "fromBackup",defaultValue="true") boolean fromBackup,
                                  @RequestParam Map<String, String> queryParameters){
        //this needs to trigger a reindex

        Optional<T> obj = getEntityService().getEntityBySomeIdentifier(id);
        if(obj.isPresent()){
            Key k = EntityWrapper.of(obj.get()).getKey();
            //delete first
            getlegacyGsrsSearchService().reindex(obj.get(), true);
            //TODO: invent response
            
            return new ResponseEntity<>(id, HttpStatus.OK);
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
        so = this.instrumentSearchOptions(so);

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

    protected abstract Object createSearchResponse(List<Object> results, SearchResult result, HttpServletRequest request);



}
