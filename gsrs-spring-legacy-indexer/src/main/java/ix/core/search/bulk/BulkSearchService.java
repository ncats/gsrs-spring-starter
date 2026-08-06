package ix.core.search.bulk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import gsrs.cache.GsrsCache;
import gsrs.repository.GsrsRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.cache.CacheStrategy;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultContext;
import ix.core.search.text.TextIndexer;
import ix.core.util.EntityUtils.Key;
import ix.utils.Util;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BulkSearchService {

	@Autowired
	private GsrsCache ixCache;

	@Autowired
	protected PlatformTransactionManager transactionManager;

	private final int MAX_BULK_SUB_QUERY_COUNT = 10000;
	private ExecutorService threadPool;

	private final ConcurrentMap<String, Future<?>> bulkSearchTaskMap = new ConcurrentHashMap<>();

	public BulkSearchService() {
		this(ForkJoinPool.commonPool());
	}

	public BulkSearchService(ExecutorService tp) {
		threadPool = tp;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public SearchResultContext search(GsrsRepository gsrsRepository, SanitizedBulkSearchRequest request,
	                                  SearchOptions options, TextIndexer textIndexer, MatchViewGenerator generator) throws IOException {
		if (request == null || request.getQueries() == null || request.getQueries().isEmpty()) {
			throw new IOException("Bulk search request must contain at least one query");
		}

		List<String> normalizedQueries = new ArrayList<>(request.getQueries().size());
		Set<String> seenQueries = new HashSet<>(request.getQueries().size());
		for (String rawQuery : request.getQueries()) {
			if (rawQuery == null) {
				continue;
			}
			String trimmedQuery = rawQuery.trim();
			if (!trimmedQuery.isEmpty() && seenQueries.add(trimmedQuery)) {
				normalizedQueries.add(trimmedQuery);
			}
		}
		SanitizedBulkSearchRequest normalizedRequest = new SanitizedBulkSearchRequest();
		normalizedRequest.setQueries(normalizedQueries);
		if (normalizedQueries.isEmpty()) {
			throw new IOException("Bulk search request must contain at least one non-empty, non-duplicate query");
		}
		String hashKey = normalizedRequest.computeKey(options.getBulkSearchOnIdentifiers(), options.getFacets());

		try {

			return ixCache.getOrElse(textIndexer.lastModified() , hashKey, ()-> {

				SearchOptions optionsCopy = new SearchOptions();
				optionsCopy.parse(options.asQueryParams());
				optionsCopy.setSimpleSearchOnly(true);
				optionsCopy.setTop(MAX_BULK_SUB_QUERY_COUNT);
				optionsCopy.setSkip(0);
				optionsCopy.setBulkSearchOnIdentifiers(options.getBulkSearchOnIdentifiers());
				for (String facet : options.getFacets()) {
					optionsCopy.addFacet(facet);
				}
				optionsCopy.setFetchAll();

				BulkSearchResultProcessor processor = autowireProcessor(createBulkSearchResultProcessor());

				processor.setResults(1, rawSearch(gsrsRepository, normalizedRequest, optionsCopy, textIndexer, generator, hashKey));

				SearchResultContext ctx = processor.getContext();
				ctx.setKey(hashKey);

				return ctx;
			});

		} catch (Exception e) {
			throw new IOException("error performing search ", e);
		}
	}

	private ResultEnumeration rawSearch(GsrsRepository gsrsRepository, SanitizedBulkSearchRequest request,
	                                    SearchOptions optionsCopy, TextIndexer textIndexer, MatchViewGenerator generator, String hashKey) {
		BlockingQueue<BulkSearchResult> bq = new LinkedBlockingQueue<BulkSearchResult>(MAX_BULK_SUB_QUERY_COUNT);
		List<SearchResultSummaryRecord> summaryList = new ArrayList<>(request.getQueries().size());
		final int totalQueries = request.getQueries().size();

		BulkQuerySummary querySummary = new BulkQuerySummary.BulkQuerySummaryBuilder()
				.qTotal(totalQueries)
				.qUnMatchTotal(0)
				.searchOnIdentifiers(optionsCopy.getBulkSearchOnIdentifiers())
				.facets(optionsCopy.getFacets())
				.qFilter(optionsCopy.getFilter())
				.qSort(optionsCopy.getOrder() != null ? String.join(",", optionsCopy.getOrder()) : null)
				.build();

		boolean searchOnIdentifiers = optionsCopy.getBulkSearchOnIdentifiers();
		final int[] unmatchedCount = new int[] {0};
		final int[] completedCount = new int[] {0};

		Future<?> future = threadPool.submit(() -> {

			try {
				List<String> queries = request.getQueries();
				List<Key> keys = new ArrayList<>();
				for (String q : queries) {
					String query = preProcessQuery(q, searchOnIdentifiers);
					try {
						SearchResult result;
						keys.clear();

						result = textIndexer.search(gsrsRepository, optionsCopy, query);
						result.copyKeysTo(keys, 0, MAX_BULK_SUB_QUERY_COUNT, true);

							if(keys.isEmpty()) {
								unmatchedCount[0]++;
							}

						SearchResultSummaryRecord singleQuerySummary = new SearchResultSummaryRecord(q, query);
						if(keys.isEmpty()) {
							singleQuerySummary.setRecords(Collections.emptyList());
						}else {
							List<MatchView> list = new ArrayList<>(keys.size());
							for (Key k : keys) {
								BulkSearchResult bsr = new BulkSearchResult();
								bsr.setQuery(q);
								bsr.setKey(k);
									bq.put(bsr);
								MatchView mv = generator.generate(bsr);
								list.add(mv);
							}
							singleQuerySummary.setRecords(list);
						}
						summaryList.add(singleQuerySummary);
						completedCount[0]++;
						querySummary.setQCompleted(completedCount[0]);
						querySummary.setQMatchTotal(completedCount[0] - unmatchedCount[0]);
						querySummary.setQUnMatchTotal(unmatchedCount[0]);
						querySummary.setQFilteredTotal(completedCount[0]);
						ixCache.setRaw("BulkSearchSummary/" + hashKey, snapshotSummary(querySummary, summaryList));
					} catch (Exception e) {
						unmatchedCount[0]++;
						SearchResultSummaryRecord singleQuerySummary = new SearchResultSummaryRecord(q, query);
						List<MatchView> list = Collections.emptyList();
						singleQuerySummary.setRecords(list);
						summaryList.add(singleQuerySummary);
						completedCount[0]++;
						querySummary.setQCompleted(completedCount[0]);
						querySummary.setQMatchTotal(completedCount[0] - unmatchedCount[0]);
						querySummary.setQUnMatchTotal(unmatchedCount[0]);
						querySummary.setQFilteredTotal(completedCount[0]);
						ixCache.setRaw("BulkSearchSummary/" + hashKey, snapshotSummary(querySummary, summaryList));
						log.error("Error processing query: " + q, e);
					}
				}

			} catch (Throwable e) {
				log.error("Error in rawSearch thread", e);
			} finally {
				querySummary.setQCompleted(totalQueries);
				querySummary.setQUnMatchTotal(unmatchedCount[0]);
				querySummary.setQMatchTotal(totalQueries - unmatchedCount[0]);
				querySummary.setQFilteredTotal(totalQueries);
				querySummary.setQueries(summaryList);
				ixCache.setRaw("BulkSearchSummary/"+request.computeKey(optionsCopy.getBulkSearchOnIdentifiers(), optionsCopy.getFacets()), snapshotSummary(querySummary, summaryList));
				bulkSearchTaskMap.remove(hashKey);
				bq.add(POISON_RESULT);
			}

		});

		bulkSearchTaskMap.put(hashKey, future);

		return new ResultEnumeration(bq);

	}

	private BulkQuerySummary snapshotSummary(BulkQuerySummary summary, List<SearchResultSummaryRecord> summaryList) {
		BulkQuerySummary.BulkQuerySummaryBuilder builder = BulkQuerySummary.builder();
		builder.qTotal(summary.getQTotal())
				.qTop(summary.getQTop())
				.qSkip(summary.getQSkip())
				.qMatchTotal(summary.getQMatchTotal())
				.qUnMatchTotal(summary.getQUnMatchTotal())
				.qCompleted(summary.getQCompleted())
				.qFilteredTotal(summary.getQFilteredTotal())
				.qFilter(summary.getQFilter())
				.qSort(summary.getQSort())
				.searchOnIdentifiers(summary.isSearchOnIdentifiers())
				.facets(summary.getFacets() == null ? null : new ArrayList<>(summary.getFacets()))
				.queries(new ArrayList<>(summaryList));
		return builder.build();
	}

	protected BulkSearchResultProcessor createBulkSearchResultProcessor() {
		return new BulkSearchResultProcessor(ixCache);
	}

	protected BulkSearchResultProcessor autowireProcessor(BulkSearchResultProcessor processor) {
		return AutowireHelper.getInstance().autowireAndProxy(processor);
	}

	public Future<?> getFuture(String key) {
		return bulkSearchTaskMap.get(key);
	}

	private String preProcessQuery(String query, boolean identifiers) {

		query = query.trim();
		int colonIndex = query.indexOf(':');
		if (colonIndex > 0) {
			boolean hasExplicitField = true;
			for (int i = 0; i < colonIndex; i++) {
				char ch = query.charAt(i);
				if (!(Character.isLetterOrDigit(ch) || ch == '_')) {
					hasExplicitField = false;
					break;
				}
			}
			if (hasExplicitField) {
				return query;
			}
		}
		// 2. remove existing quotes
		if (query.startsWith("\"")) {
			query = query.substring(1);
		}
		if (query.endsWith("\"")) {
			query = query.substring(0, query.length() - 1);
		}

		if(identifiers) {
			// 3. remove any explicit signifiers if present
			if (query.startsWith("^")) {
				query = query.substring(1);
			}
			if (query.endsWith("$")) {
				query = query.substring(0, query.length() - 1);
			}
			return "\"^" + query + "$\"";
		}else {
			return "\"" + query + "\"";
		}
	}

	@Data
	public static class SanitizedBulkSearchRequest{

		private List<String> queries;

		private String computeHash(boolean identifers, List<String> facets) {
			int hashInt = Objects.hash(queries, facets, identifers);
			return Integer.toHexString(hashInt);
		}

		public String computeKey(boolean identifers, List<String> facets){
			return Util.sha1("bulk/" + computeHash(identifers, facets));
		}
	}

	public static final BulkSearchResult POISON_RESULT = new BulkSearchResult();


	public static class ResultEnumeration implements Enumeration<BulkSearchResult> {
		final BlockingQueue<BulkSearchResult> queue;
		BulkSearchResult next;

		public ResultEnumeration (BlockingQueue<BulkSearchResult> queue) {
			this.queue = queue;
			if(queue==null){
				next=POISON_RESULT;
			}else{
				next ();
			}
		}

		void next () {
			try {
				next = queue.take();
			}
			catch (Exception ex) {
				log.error(ex.getMessage(), ex);
				next = POISON_RESULT; // terminate
			}
		}

		public boolean hasMoreElements () {
			return next != POISON_RESULT;
		}

		public BulkSearchResult nextElement () {
			if(!hasMoreElements()){
				throw new NoSuchElementException();
			}
			BulkSearchResult current = next;
			next ();
			return current;
		}
	}

	@Data
	@Builder
	@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
	@CacheStrategy(evictable=false)
	public static class BulkQuerySummary{
		int qTotal;
		int qTop;
		int qSkip;
		int qMatchTotal;
		int qUnMatchTotal;
		int qCompleted;
		int qFilteredTotal;
		String qFilter;
		String qSort;
		boolean searchOnIdentifiers;
		List<String> facets;
		List<SearchResultSummaryRecord> queries;

		public static BulkQuerySummaryBuilder builder() {return new BulkQuerySummaryBuilder();}

	}

	@Scheduled(fixedRateString = "${scheduler.bulkSearch.fixedRate:10800000}")
	public void cleanUpCompletedTasks() {

		log.info("Remove completed Bulk Search tasks in taskmap");
		Iterator<Map.Entry<String, Future<?>>> iterator = bulkSearchTaskMap.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, Future<?>> entry = iterator.next();
			Future<?> future = entry.getValue();

			// If the task is done (completed or cancelled), remove it from the map
			if (future.isDone() || future.isCancelled()) {
				iterator.remove();
			}
		}
	}

	/**
	 * Returns the {@link BulkQuerySummary} for a completed bulk search without
	 * iterating through results. The summary is populated once the background
	 * search task finishes. Use {@link #getFuture(String)} to check if the task
	 * is still running before calling this method.
	 *
	 * @param hashKey the key returned by {@link SanitizedBulkSearchRequest#computeKey}
	 * @return the summary, or {@code null} if the search has not finished yet or the key is unknown
	 */
	public BulkQuerySummary getSummary(String hashKey) {
		return (BulkQuerySummary) ixCache.getRaw("BulkSearchSummary/" + hashKey);
	}

	public Map<String, String> getBulkSearchTaskMap(){
		Map<String, String> taskMap = new HashMap<String,String>();
		for (Map.Entry<String, Future<?>> entry : bulkSearchTaskMap.entrySet()) {
			Future<?> future = entry.getValue();
			String status;
			if(future.isCancelled()) {
				status = "cancelled";
			}else if(future.isDone()) {
				status = "completed";
			}else {
				status = "running";
			}
			taskMap.put(entry.getKey(), status);
		}
		return taskMap;
	}

}
