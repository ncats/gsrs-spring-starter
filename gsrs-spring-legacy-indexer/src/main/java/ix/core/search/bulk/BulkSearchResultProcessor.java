package ix.core.search.bulk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gsrs.cache.GsrsCache;
import ix.core.search.SearchResultProcessor;
import ix.core.util.EntityUtils.Key;

public class BulkSearchResultProcessor<T> extends SearchResultProcessor<BulkSearchResult, T> {

	private GsrsCache ixCache;
	private Map<Key, Map<String, Object>> batchedContextUpdates = new HashMap<>();

	public BulkSearchResultProcessor(GsrsCache ixCache) {
		this.ixCache = ixCache;
	}

	@Override
	public T instrument(BulkSearchResult r) throws Exception {
		Map<String, Object> contextMap = batchedContextUpdates.get(r.getKey());
		if (contextMap == null) {
			contextMap = new HashMap<>();
			batchedContextUpdates.put(r.getKey(), contextMap);
		}
		@SuppressWarnings("unchecked")
		List<String> queries = (List<String>) contextMap.get("queries");
		if (queries == null) {
			queries = new ArrayList<>();
			contextMap.put("queries", queries);
			getContext().add(r.getKey());
		}
		queries.add(r.getQuery());

		return null;
	}

	/**
	 * Flush all batched context updates to the cache.
	 * Call this after all results have been instrumented.
	 */
	public void flushContextUpdates() {
		for (Map.Entry<Key, Map<String, Object>> entry : batchedContextUpdates.entrySet()) {
			ixCache.setMatchingContext(this.getContext().getId(), entry.getKey().toRootKey(), entry.getValue());
		}
		batchedContextUpdates.clear();
	}

	@Override
	public void afterProcess() throws Exception {
		flushContextUpdates();
	}
}