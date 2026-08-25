package gsrs.search;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.Predicate;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import gsrs.cache.GsrsCache;
import gsrs.controller.GetGsrsRestApiMapping;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.controller.GsrsRestApiController;
import gsrs.repository.ETagRepository;
import ix.core.models.BaseModel;
import ix.core.models.ETag;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultContext;
import ix.core.search.bulk.BulkSearchService.BulkQuerySummary;
import ix.core.search.bulk.SearchResultSummaryRecord;
import ix.core.util.EntityUtils;
import ix.core.util.pojopointer.PojoPointer;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;
import ix.core.models.Facet; 

@ExposesResourceFor(SearchResultContext.class)
@Slf4j
@GsrsRestApiController(context ="status")
public class SearchResultController {

    @Autowired
    private GsrsCache gsrsCache;
    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private ETagRepository eTagRepository;
    

    @GetGsrsRestApiMapping(value = {"({key})","/{key}"})
    public ResponseEntity<Object> getSearchResultStatus(@PathVariable("key") String key,
                                                        @RequestParam(required = false, defaultValue = "10") int top,
                                                        @RequestParam(required = false, defaultValue = "0") int skip,
                                                        @RequestParam(required = false, defaultValue = "10") int fdim,
                                                        @RequestParam(required = false, defaultValue = "") String field,
                                                        @RequestParam Map<String, String> queryParameters){
        SearchResultContext.SearchResultContextOrSerialized possibleContext=getContextForKey(key);
        if(possibleContext!=null){
            if (possibleContext.hasFullContext()) {
                SearchResultContext ctx=possibleContext.getContext()
                        .getFocused(top, skip, fdim, field);

                return new ResponseEntity<>(ctx, HttpStatus.OK);
            }else if(possibleContext.getSerialized() !=null){
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", possibleContext.getSerialized().generatingPath);
                return new ResponseEntity<>(headers,HttpStatus.FOUND);
            }
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }
    @Transactional
    @GetGsrsRestApiMapping(value = {"({key})/results","/{key}/results"})
    public ResponseEntity<Object> getSearchResultContextResult(@PathVariable("key") String key,
                                                        @RequestParam(required = false, defaultValue = "10") int top,
                                                        @RequestParam(required = false, defaultValue = "0") int skip,                                                        
                                                        @RequestParam(required = false, defaultValue = "10") int fdim,
                                                        @RequestParam(required = false, defaultValue = "") String field,
                                                        @RequestParam(required = false) String query,
                                                        @RequestParam(required = false, defaultValue = "true") boolean includeSummary,
                                                        @RequestParam MultiValueMap<String, String> queryParameters,
                                                               HttpServletRequest request) throws URISyntaxException {
    	
    	//default qTop 100
    	
    	int qTop = Integer.parseInt(queryParameters.getOrDefault("qTop", Arrays.asList("100")).get(0));
    	int qSkip = Integer.parseInt(queryParameters.getOrDefault("qSkip", Arrays.asList("0")).get(0));
    	String qSort = queryParameters.getFirst("qSort");
    	String qFilter = queryParameters.getFirst("qFilter");

        SearchResultContext.SearchResultContextOrSerialized possibleContext = getContextForKey(key);
        if(possibleContext ==null){
            return gsrsControllerConfiguration.handleNotFound(queryParameters.toSingleValueMap());
        }
        if(!possibleContext.hasFullContext()){
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", possibleContext.getSerialized().generatingPath);
            return new ResponseEntity<>(headers,HttpStatus.FOUND);
        }
        
          
        
        SearchResultContext ctx=possibleContext.getContext();

        
        //Play used a Map<String,String[]> while Spring uses a MultiMap<String,String>
        Map<String, String[]> paramMap = new java.util.LinkedHashMap<>(queryParameters.size());
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            List<String> values = entry.getValue();
            paramMap.put(entry.getKey(), values.toArray(new String[values.size()]));
        }

        // if query is null, add q parameter
        if(query == null){
            query = Optional.ofNullable(paramMap.getOrDefault("q",null)).filter(v->v!=null).map(v->v[0]).orElse(null);
        }

        SearchRequest searchRequest = new SearchRequest.Builder()
                .top(top)
                .skip(skip)
                .fdim(fdim)
                .withParameters(Util.reduceParams(paramMap,
                        "facet", "sideway", "order"))
                .query(query) //TODO: Refactor this
                .build();



        SearchResult results = ctx.getAdapted(searchRequest);

        PojoPointer pp = PojoPointer.fromURIPath(field);

        List resultSet = new ArrayList();

        SearchOptions so = searchRequest.getOptions();

        String viewType=queryParameters.getFirst("view");
        String viewField=queryParameters.getFirst("viewfield");
        final String FIELD_ID = "id";
        final String FIELD_FACET = "facet";
        boolean keyView = "key".equals(viewType)? true:false;
        if(keyView){
        	if(viewField==null) {
        		List<ix.core.util.EntityUtils.Key> klist=new ArrayList<>();
        		results.copyKeysTo(klist, so.getSkip(), so.getTop(), true);
        		resultSet=klist;
        	}else if(FIELD_ID.equals(viewField)){
        		List<ix.core.util.EntityUtils.Key> klist=new ArrayList<>();
        		results.copyKeysTo(klist, so.getSkip(), so.getTop(), true);
        		List<String> idList = new ArrayList<>(klist.size());
        		for (ix.core.util.EntityUtils.Key item : klist) {
        			idList.add(item.getIdString());
        		}
        		resultSet = idList;
        	}
        }
        else{
            results.copyTo(resultSet, so.getSkip(), so.getTop(), true);
            for (Object s : resultSet) { 
            	if(s instanceof BaseModel) {
            		((BaseModel)s).setMatchContextProperty(gsrsCache.getMatchingContextByContextID(ctx.getId(), EntityUtils.EntityWrapper.of(s).getKey().toRootKey()));
            	}
            }
        }

        int count = resultSet.size();

        Object ret= EntityUtils.EntityWrapper.of(resultSet)
                .at(pp)
                .get()
                .getValue();

        final ETag etag = new ETag.Builder()
                .fromRequest(request)
                .options(searchRequest.getOptions())
                .count(count)
                .total(results.getCount())
                .sha1(Util.sha1(ctx.getKey()))
                .build();

        eTagRepository.saveAndFlush(etag); //Always save?
        
        if(keyView && FIELD_ID.equals(viewField)) {
        	etag.setContent(resultSet);
        	return new ResponseEntity<>(etag, HttpStatus.OK);
        	
        }else if(keyView && FIELD_FACET.equals(viewField)) {
        	List<Facet> facetList = results.getFacets();
        	String facetLabel = queryParameters.getFirst("facetlabel");
        	if(facetLabel!=null) {
        		List<Facet> filteredList = new ArrayList<>(facetList.size());
        		for (Facet facet : facetList) {
        			if (facet.getName().equalsIgnoreCase(facetLabel)) {
        				filteredList.add(facet);
        			}
        		}
        		etag.setFacets(filteredList);
        				
        	}else {
        		etag.setFacets(facetList);
        	}
        	return new ResponseEntity<>(etag, HttpStatus.OK);
        }
                
        etag.setFacets(results.getFacets());
        etag.setContent(ret);
        etag.setFieldFacets(results.getFieldFacets()); 
  
        if(includeSummary && results.getSummary()!= null)
        	etag.setSummary(getPagedSummary(results.getSummary(), qTop, qSkip, qFilter, qSort));
        
        //TODO Filters and things

        return new ResponseEntity<>(etag, HttpStatus.OK);

    }

    private BulkQuerySummary getPagedSummary(BulkQuerySummary savedSummary, int qTop, int qSkip, String qFilter, String qSort) {
	
		BulkQuerySummary.BulkQuerySummaryBuilder builder = BulkQuerySummary.builder();
		builder.qTotal(savedSummary.getQTotal())
			   .qTop(qTop)
			   .qSkip(qSkip)
			   .qMatchTotal(savedSummary.getQMatchTotal())
			   .qUnMatchTotal(savedSummary.getQUnMatchTotal())
			   .qCompleted(savedSummary.getQCompleted())
			   .qRunningTotal(savedSummary.getQRunningTotal())
			   .grossMatchTotal(savedSummary.getGrossMatchTotal())
			   .totalRecordsProcessing(savedSummary.getTotalRecordsProcessing())
			   .completedRecordsSoFar(savedSummary.getCompletedRecordsSoFar())
			   .qFilteredTotal(savedSummary.getQTotal())
			   .searchOnIdentifiers(savedSummary.isSearchOnIdentifiers());
		
		List<SearchResultSummaryRecord> queriesList = savedSummary.getQueries();

		Comparator<SearchResultSummaryRecord> comp= null;

		boolean rev = false;
		String sortOn = null;

		if(qSort!=null && !(qSort.trim().equals(""))){
			 rev = qSort.startsWith("$"); // $ will be reverse sort, all other characters are normal sort.
			 sortOn=qSort.substring(1);
		}

		if(sortOn!=null) {
			if(sortOn.equalsIgnoreCase("records_length")) {
				comp= Comparator.comparing((sr)->((SearchResultSummaryRecord)sr).getRecordCount());
			}else if(sortOn.equalsIgnoreCase("searchTerm")) {
				comp= Comparator.comparing((sr)->((SearchResultSummaryRecord)sr).getSearchTerm());	
			}
			if(comp!=null) {
				builder.qSort(qSort);
			}
		}
		if(rev&&comp!=null)comp=comp.reversed();
		
		Predicate<SearchResultSummaryRecord> filter = null;
		if(qFilter!=null) {
			String qf=qFilter.toLowerCase();
			
			if(qf.startsWith("records_length:")) {
				int count = parseFilterValue(qf, ':');
				if(count>=0) {
					int fcount=count;
					filter = (sr)-> ((SearchResultSummaryRecord)sr).getRecordCount()==fcount;
				}
			}else if(qf.startsWith("records_length>")) {
				int count = parseFilterValue(qf, '>');
				if(count>=0) {
					int fcount=count;
					filter = (sr)-> ((SearchResultSummaryRecord)sr).getRecordCount()>fcount;
				}
			}else if(qf.startsWith("records_length<")) {
				int count = parseFilterValue(qf, '<');
				if(count>=0) {
					int fcount=count;
					filter = (sr)-> ((SearchResultSummaryRecord)sr).getRecordCount()<fcount;
				}
			}
			if(filter!=null) {
				builder.qFilter(qFilter);
			}
		}
		
		if(filter==null && comp==null) {
			int safeSkip = Math.max(0, qSkip);
			int safeTop = Math.max(0, qTop);
			int start = Math.min(safeSkip, queriesList.size());
			int end = Math.min(start + safeTop, queriesList.size());
			if(start >= end) {
				builder.queries(Collections.emptyList());
			}else {
				builder.queries(new ArrayList<>(queriesList.subList(start, end)));
			}
		}else {
			int safeSkip = Math.max(0, qSkip);
			int safeTop = Math.max(0, qTop);
			if(comp==null) {
				Predicate<SearchResultSummaryRecord> effectiveFilter = filter == null ? (s)->true : filter;
				List<SearchResultSummaryRecord> filteredPage = new ArrayList<>(Math.min(Math.max(safeTop, 1), queriesList.size()));
				int filteredTotal = 0;
				for(SearchResultSummaryRecord record : queriesList) {
					if(!effectiveFilter.test(record)) {
						continue;
					}
					if(filteredTotal >= safeSkip && filteredPage.size() < safeTop) {
						filteredPage.add(record);
					}
					filteredTotal++;
				}
				builder.queries(filteredPage.isEmpty() ? Collections.emptyList() : filteredPage);
				builder.qFilteredTotal(filteredTotal);
			}else {
				Predicate<SearchResultSummaryRecord> effectiveFilter = filter == null ? (s)->true : filter;
				int filteredTotal = 0;
				int windowSize = safeSkip + safeTop;
				if (windowSize <= 0) {
					for (SearchResultSummaryRecord record : queriesList) {
						if (effectiveFilter.test(record)) {
							filteredTotal++;
						}
					}
					builder.queries(Collections.emptyList());
					builder.qFilteredTotal(filteredTotal);
					return builder.build();
				}
				PriorityQueue<SearchResultSummaryRecord> window = new PriorityQueue<>(windowSize, comp.reversed());
				for(SearchResultSummaryRecord record : queriesList) {
					if(!effectiveFilter.test(record)) {
						continue;
					}
					filteredTotal++;
					if(window.size() < windowSize) {
						window.offer(record);
					}else if(comp.compare(record, window.peek()) < 0) {
						window.poll();
						window.offer(record);
					}
				}
				List<SearchResultSummaryRecord> filtered = new ArrayList<>(window);
				filtered.sort(comp);
				int start = Math.min(safeSkip, filtered.size());
				int end = Math.min(start + safeTop, filtered.size());
				if(start >= end) {
					builder.queries(Collections.emptyList());
				}else {
					builder.queries(new ArrayList<>(filtered.subList(start, end)));
				}
				builder.qFilteredTotal(filteredTotal);
			}
		}
    	
    	return builder.build();
    }

    private int parseFilterValue(String filter, char separator) {
    	int separatorIndex = filter.indexOf(separator);
    	if(separatorIndex < 0 || separatorIndex + 1 >= filter.length()) {
    		return -1;
    	}
    	try {
    		return Integer.parseInt(filter.substring(separatorIndex + 1));
    	}catch(Exception e) {
    		return -1;
    	}
    }
    
    private SearchResultContext.SearchResultContextOrSerialized getContextForKey(String key){
    	SearchResultContext context=null;
    	SearchResultContext.SerailizedSearchResultContext serial=null;
        try {
            Object value = gsrsCache.get(key);
//			System.out.println("cache value " + value);
            if (value != null) {
            	if(value instanceof SearchResultContext){
                    context = (SearchResultContext)value;
            	}else if(value instanceof SearchResult){
            		SearchResult result = (SearchResult)value;
            		context = new SearchResultContext(result);

                    log.debug("status: key="+key+" finished="+context.isFinished());
            	}
            }else{
            	String spkey  = SearchResultContext.getSerializedKey(key);
            	Object value2 = gsrsCache.getRaw(spkey);

//				System.out.println("serialized key " + spkey);
//				System.out.println("value2 " + value2);
            	if(value2 !=null && value2 instanceof SearchResultContext.SerailizedSearchResultContext){
            		serial=(SearchResultContext.SerailizedSearchResultContext) value2;
            	}
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }
	    if(context!=null){
	    	context.setKey(key);
            context.setSummary(getBulkSummaryForKey(key));
	    	return new SearchResultContext.SearchResultContextOrSerialized(context);
	    }else if(serial !=null){
	    	return new SearchResultContext.SearchResultContextOrSerialized(serial);
	    }
	    return null;
    }

    private BulkQuerySummary getBulkSummaryForKey(String key) {
        Object cached = gsrsCache.getRaw("BulkSearchSummary/" + key);
        if (cached instanceof BulkQuerySummary) {
            return (BulkQuerySummary) cached;
        }
        return null;
    }
    
}
