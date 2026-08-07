package ix.core.search.bulk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gsrs.cache.GsrsCache;
import ix.core.EntityFetcher;
import ix.core.search.SearchResultProcessor;
import ix.core.util.EntityUtils.Key;

public class BulkSearchResultProcessor<T> extends SearchResultProcessor<BulkSearchResult, T> {

	private GsrsCache ixCache;
	private Map<Key, List<String>> matches = new ConcurrentHashMap<>();

	public BulkSearchResultProcessor(GsrsCache ixCache) {
		this.ixCache = ixCache;
	}

	@Override
	public T instrument(BulkSearchResult r) throws Exception {
		List<String> queries = matches.computeIfAbsent(r.getKey(), (k) -> new ArrayList<String>());
		queries.add(r.getQuery());
		addBulkResultToSubstanceMatchContext(r);
		if (queries.size() > 1) {
			return null;
		} else {
			// todo: Evaluate lazy loading
			return (T) EntityFetcher.of(r.getKey()).call();
		}
	}

	private void addBulkResultToSubstanceMatchContext(BulkSearchResult r) {		
		Map<String, Object> map = new HashMap<>();
		map.put("queries", matches.get(r.getKey().toRootKey()));
		ixCache.setMatchingContext(this.getContext().getId(), r.getKey().toRootKey(), map);
	}
}