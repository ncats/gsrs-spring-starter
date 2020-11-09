package gsrs.controller;

import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.repository.GsrsRepository;
import ix.core.models.ETag;
import ix.core.search.SearchResult;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public abstract class EtagLegacySearchEntityController<T,I> extends AbstractLegacyTextSearchGsrsEntityController<T,I> {

    public EtagLegacySearchEntityController(String context) {
        super(context);
    }

    @Override
    protected Object createSearchResponse(List<Object> results, SearchResult result, HttpServletRequest request) {
        return saveAsEtag(results, result, request);
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
