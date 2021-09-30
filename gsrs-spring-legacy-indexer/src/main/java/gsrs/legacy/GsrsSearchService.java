package gsrs.legacy;


import ix.core.search.SearchOptions;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public interface GsrsSearchService<T> {


    SearchResult search(String query, SearchOptions options) throws IOException;

    TextIndexer.TermVectors getTermVectors(Optional<String> field) throws IOException;

    TextIndexer.TermVectors getTermVectorsFromQuery(String query, SearchOptions options, String field) throws IOException;

    SearchResult search(String query, SearchOptions options, Collection<?> subset) throws IOException;

    Map<String, List<? extends GsrsSuggestResult>> suggest(String query, int max) throws IOException;
    List<? extends GsrsSuggestResult> suggestField(String field, String query, int max) throws IOException;

    Collection<String> getSuggestFields() throws IOException;

    /**
     * timestamp of last time the service was modified.
     * @return
     */
    long getLastModified();

    ReindexService<T> getReindexService();
}
