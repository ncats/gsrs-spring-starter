package gsrs.controller;

import gsrs.controller.hateoas.IxContext;
import gsrs.legacy.GsrsSuggestResult;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.springUtils.GsrsSpringUtils;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.search.GsrsLegacySearchController;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.FacetMeta;
import ix.core.search.text.TextIndexer;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.utils.Util;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

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

    /**
     * Force a reindex of all entities of this entity type.
     * @param wipeIndex should the whole index be deleted before re-index begins;
     *                  defaults to {@code false}.
     * @return
     */
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
                List<ix.core.util.EntityUtils.Key> klist=new ArrayList<>(top.orElse(10));
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
