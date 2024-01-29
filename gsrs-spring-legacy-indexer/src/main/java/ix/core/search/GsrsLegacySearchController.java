package ix.core.search;

import gsrs.controller.GetGsrsRestApiMapping;
import ix.core.search.text.FacetMeta;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public interface GsrsLegacySearchController {

    FacetMeta searchFacetFieldDrilldownV1(@RequestParam("q") Optional<String> query,
                                          @RequestParam("field") Optional<String> field,
                                          @RequestParam("top") Optional<Integer> top,
                                          @RequestParam("skip") Optional<Integer> skip,
                                          HttpServletRequest request) throws ParseException, IOException;

    FacetMeta searchFacetFieldV1(@RequestParam("field") Optional<String> field,
                                 @RequestParam("top") Optional<Integer> top,
                                 @RequestParam("skip") Optional<Integer> skip,
                                 HttpServletRequest request) throws ParseException, IOException;

    ResponseEntity<Object> searchV1(@RequestParam("q") Optional<String> query,
                                           @RequestParam("top") Optional<Integer> top,
                                           @RequestParam("skip") Optional<Integer> skip,
                                           @RequestParam("fdim") Optional<Integer> fdim,
                                           HttpServletRequest request,
                                           @RequestParam Map<String, String> queryParameters);
    
    default SearchOptions instrumentSearchOptions(SearchOptions so) {
        return so;
    }
    
    default SearchRequest instrumentSearchRequest(SearchRequest sr) {
        SearchOptions so =instrumentSearchOptions(sr.getOptions());
        sr.setOptions(so);
        return sr;
    }

}
