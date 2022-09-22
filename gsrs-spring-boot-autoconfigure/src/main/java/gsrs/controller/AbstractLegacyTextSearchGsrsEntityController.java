package gsrs.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.nih.ncats.common.Tuple;
import gsrs.cache.GsrsCache;
import gsrs.controller.hateoas.IxContext;
import gsrs.legacy.GsrsSuggestResult;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.security.hasAdminRole;
import gsrs.services.TextService;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.search.GsrsLegacySearchController;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultSummaryRecord;
import ix.core.search.text.FacetMeta;
import ix.core.search.text.TextIndexer;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.Key;
import ix.ginas.models.GinasCommonData;
import ix.utils.Util;


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
    
    @Autowired
    private TextService textService;
    
    @Autowired    
	GsrsCache gsrscache;

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
    
    @PostGsrsRestApiMapping(value="/@bulkQuery")
    public ResponseEntity<String> saveQueryList(@RequestBody String query,
    									@RequestParam("top") Optional<Integer> top,
  										@RequestParam("skip") Optional<Integer> skip,
  										HttpServletRequest request){
    	
    	int qTop = 100, qSkip = 0;
    	if(top.isPresent()) 
    		qTop = top.get();
    	if(skip.isPresent()) 
    		qSkip = top.get();
    	    	
    	List<String> queries = Arrays.asList(query.split("\\r?\\n|\\r"));
  	  
    	List<String> list = queries.stream()    			
    			.map(q->q.trim())
    			.filter(q->q.length()>0)
    			.distinct()
    			.collect(Collectors.toList());    	
    	
    	String queryStringToSave = list.toString();
    	Long id = textService.saveTextString("bulkSearch", queryStringToSave.substring(1, queryStringToSave.length()-1));
    	    	
    	String returnJsonSrting = createJson(id, qTop, qSkip, list, request.getRequestURL().toString()+"?"+request.getQueryString());    	
 
        return new ResponseEntity<>(returnJsonSrting, HttpStatus.OK);
    }

    @GetGsrsRestApiMapping(value="/@bulkQuery")
    public ResponseEntity<String> getQueryList(@RequestParam String id,
    										   @RequestParam("top") Optional<Integer> top,
    										   @RequestParam("skip") Optional<Integer> skip,
    										   HttpServletRequest request){    	
    	
    	String queryString = textService.getText(id); 
    	int qTop = 100, qSkip = 0;
    	if(top.isPresent()) 
    		qTop = top.get();
    	if(skip.isPresent()) 
    		qSkip = top.get();
    	List<String> queries = Arrays.asList(queryString.split("\\s*,\\s*"));
    	List<String> list = queries.stream()
    								.map(p->p.trim())
    								.collect(Collectors.toList());    	
    	
    	String returnJson = createJson(Long.parseLong(id), qTop, qSkip, list, request.getRequestURL().toString()+"?"+request.getQueryString());
        return new ResponseEntity<>(returnJson, HttpStatus.OK);
    }
    
    @DeleteGsrsRestApiMapping(value="/@bulkQuery")
    public ResponseEntity<String> deleteQueryList(@RequestParam String id){    	
    	textService.deleteText(id); 	    	
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @GetGsrsRestApiMapping(value = "/bulkSearch", apiVersions = 1)
    public ResponseEntity<Object> bulkSearch(@RequestParam("bulkQID") String queryListID,
    									   @RequestParam("q") Optional<String> query,
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
            result = getlegacyGsrsSearchService().bulkSearch(queryListID, textService.getText(queryListID), searchRequest.getQuery(), searchRequest.getOptions() );
        } catch (Exception e) {
        	e.printStackTrace();        	
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

        List<GinasCommonData> matches = new ArrayList<>();
        result.copyTo(matches, 0, result.size(), true); 
        matches.forEach(data -> {    
        	Key key = EntityUtils.EntityWrapper.of(data).getKey().toRootKey();
        	Map<String,Object> queryMap = gsrscache.getMatchingContextByContextID("matchBulkSearchQueries"+ queryListID, key);
        	if(queryMap!=null && !queryMap.isEmpty())
        		data.setMatchContextProperty(queryMap);
        });
        
        
        List<SearchResultSummaryRecord> summary = getSummary("matchBulkSearchStatistics" + queryListID);
        result.setSummary(summary);
        
        //even if list is empty we want to return an empty list not a 404
        ResponseEntity<Object> ret= new ResponseEntity<>(createSearchResponse(results, result, request), HttpStatus.OK);
        return ret;
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
    
    private List<SearchResultSummaryRecord> getSummary(String key) {
    	
    	List<Tuple<String,List<Key>>> statistics = (List<Tuple<String,List<Key>>>)gsrscache.getRaw(key);
    	List<SearchResultSummaryRecord> summary = statistics.stream().map(q->{
    			SearchResultSummaryRecord record = new SearchResultSummaryRecord(q.k());
    			record.getRecordUNIIs().addAll(q.v());    		
    			return record;})
    			.collect(Collectors.toList());
    	return summary;
    }
    
    protected abstract Object createSearchResponse(List<Object> results, SearchResult result, HttpServletRequest request);



}
