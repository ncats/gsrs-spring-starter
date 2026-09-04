package gsrs.search;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;

import gsrs.controller.GetGsrsRestApiMapping;
import gsrs.controller.GsrsRestApiController;
import gsrs.cache.GsrsCache;
import gsrs.services.TextService;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResultContext;
import ix.core.search.bulk.BulkSearchService;
import ix.core.search.bulk.BulkSearchService.BulkQuerySummary;
import ix.core.search.bulk.CrossEntityBulkSearchService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GsrsRestApiController(context = "search")
public class CrossEntityBulkSearchController {

    @Autowired
    private CrossEntityBulkSearchService crossEntityBulkSearchService;

    @Autowired
    private GsrsCache gsrscache;

    @Autowired
    private TextService textService;

    @GetGsrsRestApiMapping(value = {"/bulkSearch/all", "/bulkSearch/cross"}, apiVersions = 1)
    public ResponseEntity<Object> bulkSearchAcrossEntities(@RequestParam("bulkQID") String queryListID,
                                                           @RequestParam("q") Optional<String> query,
                                                           @RequestParam("top") Optional<Integer> top,
                                                           @RequestParam("skip") Optional<Integer> skip,
                                                           @RequestParam("qTop") Optional<Integer> qTop,
                                                           @RequestParam("qSkip") Optional<Integer> qSkip,
                                                           @RequestParam("fdim") Optional<Integer> fdim,
                                                           @RequestParam(value = "contexts", required = false) List<String> contexts,
                                                           HttpServletRequest request,
                                                           @RequestParam MultiValueMap<String, String> queryParameters) {
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .query(query.orElse(null));

        top.ifPresent(builder::top);
        skip.ifPresent(builder::skip);
        fdim.ifPresent(builder::fdim);
        qTop.ifPresent(builder::qTop);
        qSkip.ifPresent(builder::qSkip);

        SearchRequest searchRequest = builder.withParameters(request.getParameterMap()).build();
        SearchOptions searchOptions = searchRequest.getOptions();

        BulkSearchService.SanitizedBulkSearchRequest sanitizedRequest = new BulkSearchService.SanitizedBulkSearchRequest();
        try {
            List<String> queries = gsrscache.getOrElse("/BulkID/" + queryListID, () -> {
                String queryString = textService.getText(queryListID);
                if (queryString == null || queryString.trim().isEmpty()) {
                    throw new RuntimeException("Cannot find bulk query ID.");
                }
                return BulkSearchService.parseNormalizedQueries(queryString);
            });
            sanitizedRequest.setQueries(queries);
        } catch (Exception e) {
            log.error("Could not load bulk query list {}", queryListID, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }

        try {
            Collection<String> contextFilter = contexts;
            SearchResultContext resultContext = crossEntityBulkSearchService.search(sanitizedRequest, searchOptions, contextFilter);
            if (resultContext.getKey() != null) {
                BulkQuerySummary runningSummary = getBulkSummaryForKey(resultContext.getKey());
                if (runningSummary != null) {
                    resultContext.setSummary(runningSummary);
                }
            }
            updateSearchContextGenerator(resultContext, queryParameters);
            return new ResponseEntity<>(resultContext, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Cross-entity bulk search failed", e);
            return new ResponseEntity<>("Error during cross-entity bulk search!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void updateSearchContextGenerator(SearchResultContext resultContext, MultiValueMap<String, String> queryParameters) {
        String oldURL = resultContext.getGeneratingUrl();
        if (oldURL != null && !oldURL.contains("?")) {
            StringBuilder queryParamBuilder = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
                List<String> values = entry.getValue();
                if (values == null || values.isEmpty()) {
                    continue;
                }
                if (queryParamBuilder.length() == 0) {
                    queryParamBuilder.append("?");
                } else {
                    queryParamBuilder.append("&");
                }
                queryParamBuilder.append(entry.getKey()).append("=").append(values.get(0));
            }
            resultContext.setGeneratingUrl(oldURL + queryParamBuilder);
        }
    }

    private BulkQuerySummary getBulkSummaryForKey(String key) {
        Object cached = gsrscache.getRaw("BulkSearchSummary/" + key);
        if (cached instanceof BulkQuerySummary) {
            return (BulkQuerySummary) cached;
        }
        return null;
    }
}
