package gsrs.controller;

import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.springUtils.GsrsSpringUtils;
import ix.core.models.ETag;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.FacetMeta;
import ix.core.search.text.TextIndexer;
import ix.utils.Util;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.function.EntityResponse;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Extension to AbstractGsrsEntityController that adds support for the legacy TextIndexer
 * and related search routes that would use it including {@code /@facets} , {@code /search} and {@code /search/@facets} .
 *
 * @param <T>
 * @param <I>
 */
public abstract class AbstractLegacyTextSearchGsrsEntityController<T, I> extends AbstractGsrsEntityController<T,I> {
    public AbstractLegacyTextSearchGsrsEntityController(String context) {
        super(context);
    }

    @PostGsrsRestApiMapping(value="/@reindex", apiVersions = 1)
    public ResponseEntity forceFullReindex(){
        getlegacyGsrsSearchService().reindexAndWait();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @GetGsrsRestApiMapping(value = "/search/@facets", apiVersions = 1)
    public FacetMeta searchFacetFieldDrilldownV1(@RequestParam("q") Optional<String> query,
                                                 @RequestParam("field") Optional<String> field,
                                                 @RequestParam("top") Optional<Integer> top,
                                                 @RequestParam("skip") Optional<Integer> skip,
                                                 HttpServletRequest request) throws ParseException, IOException {
        SearchOptions so = new SearchOptions.Builder()
                .kind(getEntityClass())
                .top(Integer.MAX_VALUE) // match Play GSRS
                .fdim(10)
                .fskip(0)
                .ffilter("")
                .withParameters(request.getParameterMap())
                .build();

        TextIndexer.TermVectors tv= getlegacyGsrsSearchService().getTermVectorsFromQuery(query.orElse(null), so, field.orElse(null));
        return tv.getFacet(so.getFdim(), so.getFskip(), so.getFfilter(), GsrsSpringUtils.getFullUrlFrom(request));


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

        TextIndexer.TermVectors tv = getlegacyGsrsSearchService().getTermVectors(field);
        return tv.getFacet(so.getFdim(), so.getFskip(), so.getFfilter(), GsrsSpringUtils.getFullUrlFrom(request));

    }

    /**
     * Get the implementation of {@link LegacyGsrsSearchService} for this entity type.
     * @return
     */
    protected abstract LegacyGsrsSearchService<T> getlegacyGsrsSearchService();

    @GetGsrsRestApiMapping(value = "/search", apiVersions = 1)
    public ResponseEntity<Object> searchV1(@RequestParam("q") Optional<String> query,
                                           @RequestParam("top") Optional<Integer> top,
                                           @RequestParam("skip") Optional<Integer> skip,
                                           @RequestParam("fdim") Optional<Integer> fdim,
                                           HttpServletRequest request,
                                           @RequestParam Map<String, String> queryParameters){
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .query(query.orElse(null))
                .kind(getEntityClass());

        top.ifPresent( t-> builder.top(t));
        skip.ifPresent( t-> builder.skip(t));
        fdim.ifPresent( t-> builder.fdim(t));

        SearchRequest searchRequest = builder.withParameters(request.getParameterMap())
                .build();

/*
SearchRequest req = builder
                .top(top)
                .skip(skip)
                .fdim(fdim)
                .kind(kind)
                .withRequest(request()) // I don't like this,
                                        // I like being explicit,
                                        // but it's ok for now
                .query(q)
                .build();
 */
        SearchResult result = null;
        try {
            result = getlegacyGsrsSearchService().search(searchRequest.getQuery(), searchRequest.getOptions() );
        } catch (Exception e) {
            return getGsrsControllerConfiguration().handleError(e, queryParameters);
        }
        List<Object> results = new ArrayList<>();

        result.copyTo(results, 0, top.orElse(10), true); //this looks wrong, because we're not skipping
        //anything, but it's actually right,
        //because the original request did the skipping.
        //This mechanism should probably be worked out
        //better, as it's not consistent.


        //even if list is empty we want to return an empty list not a 404
        return new ResponseEntity<>(saveAsEtag(results, result, request), HttpStatus.OK);
    }

    private static ETag saveAsEtag(List<Object> results, SearchResult result, HttpServletRequest request) {
        final ETag etag = new ETag.Builder()
                .fromRequest(request)
                .options(result.getOptions())
                .count(results.size())
                .total(result.getCount())

                .sha1OfRequest(request, "q", "facet")
                .build();

//        if(request().queryString().get("export") ==null) {
//            etag.save();
//        }
        etag.setContent(results);
        etag.setSponosredResults(result.getSponsoredMatches());
        etag.setFacets(result.getFacets());
        etag.setFieldFacets(result.getFieldFacets());
        etag.setSelected(result.getOptions().getFacets(), result.getOptions().isSideway());


        return etag;
    }

}
