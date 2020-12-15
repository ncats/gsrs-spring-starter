package gsrs.controller;


import ix.core.models.ETag;
import ix.core.search.SearchResult;
import ix.core.util.EntityUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class EtagLegacySearchEntityController<C extends EtagLegacySearchEntityController,  T,I> extends AbstractLegacyTextSearchGsrsEntityController<C, T,I> {

//    public EtagLegacySearchEntityController(String context, Pattern pattern) {
//        super(context, pattern);
//    }
//    public EtagLegacySearchEntityController(String context, IdHelper idHelper) {
//        super(context, idHelper);
//    }
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
            //TODO add save and export support
//        if(request().queryString().get("export") ==null) {
//            etag.save();
//        }
        //content is transient so don't need to worry about transforming results
        String view = request.getParameter("view");
        if("key".equals(view)){
            etag.setContent(results.stream().map(e->  {
                Optional<EntityUtils.Key> opt = EntityUtils.EntityWrapper.of(e).getOptionalKey();
                if(opt.isPresent()){
                    return opt.get();
                }
                return e;
            }
            ).collect(Collectors.toList()));
        }else {
            etag.setContent(results);
        }
        etag.setSponosredResults(result.getSponsoredMatches());
        etag.setFacets(result.getFacets());
        etag.setFieldFacets(result.getFieldFacets());
        etag.setSelected(result.getOptions().getFacets(), result.getOptions().isSideway());


        return etag;
    }
}
