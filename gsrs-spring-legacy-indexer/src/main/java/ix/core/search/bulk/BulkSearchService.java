package ix.core.search.bulk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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

	private static final int MAX_BULK_SUB_QUERY_COUNT = 10000;
	private static final int BULK_SUMMARY_UPDATE_EVERY_QUERIES = 25;
	private static final long BULK_SUMMARY_UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(400);
	private static final int LARGE_BULK_PROGRESS_UPDATE_EVERY_QUERIES = 100;
	private static final long LARGE_BULK_PROGRESS_UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(800);
	private static final int VERY_LARGE_BULK_PROGRESS_UPDATE_EVERY_QUERIES = 250;
	private static final long VERY_LARGE_BULK_PROGRESS_UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(1500);
	private static final int LARGE_BULK_QUERY_THRESHOLD = 1000;
	private static final int MAX_PARALLEL_BULK_QUERY_WORKERS = 8;
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

		List<String> normalizedQueries = normalizeQueries(request.getQueries());
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
		final int totalQueries = request.getQueries().size();
		final String summaryCacheKey = "BulkSearchSummary/" + hashKey;
		final SearchResultSummaryRecord[] summaryRecords = new SearchResultSummaryRecord[totalQueries];
		final List<SearchResultSummaryRecord> completedSummaryRecords = new ArrayList<>(totalQueries);

		BulkQuerySummary querySummary = new BulkQuerySummary.BulkQuerySummaryBuilder()
				.qTotal(totalQueries)
				.qUnMatchTotal(0)
				.qRunningTotal(0)
				.grossMatchTotal(0)
				.totalRecordsProcessing(0)
				.completedRecordsSoFar(0)
				.searchOnIdentifiers(optionsCopy.getBulkSearchOnIdentifiers())
				.facets(optionsCopy.getFacets())
				.qFilter(optionsCopy.getFilter())
				.qSort(optionsCopy.getOrder() != null ? String.join(",", optionsCopy.getOrder()) : null)
				.build();

		boolean searchOnIdentifiers = optionsCopy.getBulkSearchOnIdentifiers();
		final int[] unmatchedCount = new int[] {0};
		final int[] completedCount = new int[] {0};
		final int[] grossMatchCount = new int[] {0};
		final Set<Key> uniqueMatchedKeys = new HashSet<>();
		final long[] lastSummaryWriteNanos = new long[] {System.nanoTime()};
		final int queryWorkerCount = determineQueryWorkerCount(totalQueries);
		final boolean includeProgressQueryDetails = shouldIncludeProgressQueryDetails(totalQueries);

		Future<?> future = threadPool.submit(() -> {

			try {
				List<String> queries = request.getQueries();
				if (queryWorkerCount <= 1) {
					for (int i = 0; i < queries.size(); i++) {
						QueryOutcome outcome = executeQuery(i, queries.get(i), searchOnIdentifiers, gsrsRepository, optionsCopy, textIndexer);
						processQueryOutcome(outcome, generator, bq, uniqueMatchedKeys, grossMatchCount, querySummary, summaryRecords, completedSummaryRecords, unmatchedCount, completedCount);
						maybePersistProgress(summaryCacheKey, querySummary, completedSummaryRecords, includeProgressQueryDetails, completedCount[0], totalQueries, lastSummaryWriteNanos);
					}
				} else {
					CompletionService<QueryOutcome> completionService = new ExecutorCompletionService<>(ForkJoinPool.commonPool());
					int submitted = 0;
					int completed = 0;
					int initial = Math.min(queryWorkerCount, queries.size());
					for (; submitted < initial; submitted++) {
						final int index = submitted;
						completionService.submit(() -> executeQuery(index, queries.get(index), searchOnIdentifiers, gsrsRepository, optionsCopy, textIndexer));
					}

					while (completed < queries.size()) {
						Future<QueryOutcome> completedFuture;
						try {
							completedFuture = completionService.take();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw e;
						}
						QueryOutcome outcome;
						try {
							outcome = completedFuture.get();
						} catch (Exception e) {
							log.error("Error getting bulk query outcome", e);
							completed++;
							continue;
						}

						processQueryOutcome(outcome, generator, bq, uniqueMatchedKeys, grossMatchCount, querySummary, summaryRecords, completedSummaryRecords, unmatchedCount, completedCount);
						completed++;
						if (submitted < queries.size()) {
							final int index = submitted++;
							completionService.submit(() -> executeQuery(index, queries.get(index), searchOnIdentifiers, gsrsRepository, optionsCopy, textIndexer));
						}
						maybePersistProgress(summaryCacheKey, querySummary, completedSummaryRecords, includeProgressQueryDetails, completedCount[0], totalQueries, lastSummaryWriteNanos);
					}
				}

			} catch (Throwable e) {
				log.error("Error in rawSearch thread", e);
			} finally {
				List<SearchResultSummaryRecord> finalSummaryList = buildFinalSummaryRecords(summaryRecords);
				querySummary.setQCompleted(totalQueries);
				querySummary.setQUnMatchTotal(unmatchedCount[0]);
				querySummary.setQMatchTotal(totalQueries - unmatchedCount[0]);
				querySummary.setQFilteredTotal(totalQueries);
				updateSummaryCounters(querySummary, uniqueMatchedKeys.size(), grossMatchCount[0]);
				querySummary.setQueries(finalSummaryList);
				ixCache.setRaw(summaryCacheKey, snapshotSummary(querySummary, finalSummaryList, true));
				bulkSearchTaskMap.remove(hashKey);
				bq.add(POISON_RESULT);
			}

		});

		bulkSearchTaskMap.put(hashKey, future);

		return new ResultEnumeration(bq);

	}

	private QueryOutcome executeQuery(int queryIndex,
									  String originalQuery,
									  boolean searchOnIdentifiers,
									  GsrsRepository gsrsRepository,
									  SearchOptions optionsCopy,
									  TextIndexer textIndexer) {
		String processedQuery = preProcessQuery(originalQuery, searchOnIdentifiers);
		try {
			SearchResult result = textIndexer.search(gsrsRepository, optionsCopy, processedQuery);
			List<Key> keys = new ArrayList<>(256);
			result.copyKeysTo(keys, 0, MAX_BULK_SUB_QUERY_COUNT, true);
			return QueryOutcome.success(queryIndex, originalQuery, processedQuery, keys);
		} catch (Exception e) {
			return QueryOutcome.failure(queryIndex, originalQuery, processedQuery, e);
		}
	}

	private void processQueryOutcome(QueryOutcome outcome,
									 MatchViewGenerator generator,
									 BlockingQueue<BulkSearchResult> bq,
									 Set<Key> uniqueMatchedKeys,
									 int[] grossMatchCount,
									 BulkQuerySummary querySummary,
									 SearchResultSummaryRecord[] summaryRecords,
									 List<SearchResultSummaryRecord> completedSummaryRecords,
									 int[] unmatchedCount,
									 int[] completedCount) throws InterruptedException {
		if (outcome == null) {
			return;
		}

		SearchResultSummaryRecord singleQuerySummary = new SearchResultSummaryRecord(outcome.originalQuery, outcome.processedQuery);
		List<Key> keys = outcome.keys;
		if (outcome.error != null || keys == null || keys.isEmpty()) {
			unmatchedCount[0]++;
			singleQuerySummary.setRecords(Collections.emptyList());
			if (outcome.error != null) {
				log.error("Error processing query: " + outcome.originalQuery, outcome.error);
			}
		} else {
			grossMatchCount[0] += keys.size();
			List<MatchView> list = new ArrayList<>(keys.size());
			for (Key k : keys) {
				uniqueMatchedKeys.add(k);
				BulkSearchResult bsr = new BulkSearchResult();
				bsr.setQuery(outcome.originalQuery);
				bsr.setKey(k);
				bq.put(bsr);
				MatchView mv = generator.generate(bsr);
				list.add(mv);
			}
			singleQuerySummary.setRecords(list);
		}

		updateSummaryCounters(querySummary, uniqueMatchedKeys.size(), grossMatchCount[0]);
		summaryRecords[outcome.queryIndex] = singleQuerySummary;
		completedSummaryRecords.add(singleQuerySummary);
		completedCount[0]++;
		querySummary.setQCompleted(completedCount[0]);
		querySummary.setQMatchTotal(completedCount[0] - unmatchedCount[0]);
		querySummary.setQUnMatchTotal(unmatchedCount[0]);
		querySummary.setQFilteredTotal(completedCount[0]);
	}

	private void updateSummaryCounters(BulkQuerySummary querySummary, int uniqueCount, int grossCount) {
		querySummary.setQRunningTotal(uniqueCount);
		querySummary.setGrossMatchTotal(grossCount);
		querySummary.setTotalRecordsProcessing(grossCount);
		querySummary.setCompletedRecordsSoFar(grossCount);
	}

	private void maybePersistProgress(String summaryCacheKey,
									  BulkQuerySummary querySummary,
									  List<SearchResultSummaryRecord> completedSummaryRecords,
									  boolean includeQueryDetails,
									  int completedQueries,
									  int totalQueries,
									  long[] lastWriteNanos) {
		if (shouldPersistProgressSummary(completedQueries, totalQueries, lastWriteNanos[0])) {
			persistProgressSummary(summaryCacheKey, querySummary, completedSummaryRecords, includeQueryDetails);
			lastWriteNanos[0] = System.nanoTime();
		}
	}

	private int determineQueryWorkerCount(int totalQueries) {
		if (totalQueries <= 1) {
			return 1;
		}
		int cpuCount = Math.max(1, Runtime.getRuntime().availableProcessors());
		return Math.min(totalQueries, Math.min(MAX_PARALLEL_BULK_QUERY_WORKERS, cpuCount));
	}

	private void persistProgressSummary(String summaryCacheKey,
										BulkQuerySummary querySummary,
										List<SearchResultSummaryRecord> completedSummaryRecords,
										boolean includeQueryDetails) {
		List<SearchResultSummaryRecord> summaryList = includeQueryDetails
				? completedSummaryRecords
				: Collections.emptyList();
		ixCache.setRaw(summaryCacheKey, snapshotSummary(querySummary, summaryList, includeQueryDetails));
	}

	private List<SearchResultSummaryRecord> buildFinalSummaryRecords(SearchResultSummaryRecord[] records) {
		List<SearchResultSummaryRecord> ordered = new ArrayList<>(records.length);
		for (SearchResultSummaryRecord record : records) {
			if (record != null) {
				ordered.add(record);
			}
		}
		return ordered;
	}

	private static class QueryOutcome {
		final int queryIndex;
		final String originalQuery;
		final String processedQuery;
		final List<Key> keys;
		final Exception error;

		private QueryOutcome(int queryIndex, String originalQuery, String processedQuery, List<Key> keys, Exception error) {
			this.queryIndex = queryIndex;
			this.originalQuery = originalQuery;
			this.processedQuery = processedQuery;
			this.keys = keys;
			this.error = error;
		}

		static QueryOutcome success(int queryIndex, String originalQuery, String processedQuery, List<Key> keys) {
			return new QueryOutcome(queryIndex, originalQuery, processedQuery, keys, null);
		}

		static QueryOutcome failure(int queryIndex, String originalQuery, String processedQuery, Exception error) {
			return new QueryOutcome(queryIndex, originalQuery, processedQuery, Collections.emptyList(), error);
		}
	}

	private boolean shouldPersistProgressSummary(int completedQueries, int totalQueries, long lastWriteNanos) {
		if (completedQueries <= 1 || completedQueries >= totalQueries) {
			return true;
		}
		int progressUpdateEvery = getProgressUpdateEveryQueries(totalQueries);
		if (completedQueries % progressUpdateEvery == 0) {
			return true;
		}
		return System.nanoTime() - lastWriteNanos >= getProgressUpdateIntervalNanos(totalQueries);
	}

	private boolean shouldIncludeProgressQueryDetails(int totalQueries) {
		return totalQueries <= LARGE_BULK_QUERY_THRESHOLD;
	}

	private int getProgressUpdateEveryQueries(int totalQueries) {
		if (totalQueries > 5000) {
			return VERY_LARGE_BULK_PROGRESS_UPDATE_EVERY_QUERIES;
		}
		if (totalQueries > LARGE_BULK_QUERY_THRESHOLD) {
			return LARGE_BULK_PROGRESS_UPDATE_EVERY_QUERIES;
		}
		return BULK_SUMMARY_UPDATE_EVERY_QUERIES;
	}

	private long getProgressUpdateIntervalNanos(int totalQueries) {
		if (totalQueries > 5000) {
			return VERY_LARGE_BULK_PROGRESS_UPDATE_INTERVAL_NANOS;
		}
		if (totalQueries > LARGE_BULK_QUERY_THRESHOLD) {
			return LARGE_BULK_PROGRESS_UPDATE_INTERVAL_NANOS;
		}
		return BULK_SUMMARY_UPDATE_INTERVAL_NANOS;
	}

	private BulkQuerySummary snapshotSummary(BulkQuerySummary summary, List<SearchResultSummaryRecord> summaryList, boolean includeQueryDetails) {
		BulkQuerySummary.BulkQuerySummaryBuilder builder = BulkQuerySummary.builder();
		builder.qTotal(summary.getQTotal())
				.qTop(summary.getQTop())
				.qSkip(summary.getQSkip())
				.qMatchTotal(summary.getQMatchTotal())
				.qUnMatchTotal(summary.getQUnMatchTotal())
				.qCompleted(summary.getQCompleted())
				.qRunningTotal(summary.getQRunningTotal())
				.grossMatchTotal(summary.getGrossMatchTotal())
				.totalRecordsProcessing(summary.getTotalRecordsProcessing())
				.completedRecordsSoFar(summary.getCompletedRecordsSoFar())
				.qFilteredTotal(summary.getQFilteredTotal())
				.qFilter(summary.getQFilter())
				.qSort(summary.getQSort())
				.searchOnIdentifiers(summary.isSearchOnIdentifiers())
				.facets(summary.getFacets() == null ? null : new ArrayList<>(summary.getFacets()))
				.queries(includeQueryDetails ? new ArrayList<>(summaryList) : Collections.emptyList());
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

	public static List<String> normalizeQueries(List<String> rawQueries) {
		if (rawQueries == null || rawQueries.isEmpty()) {
			return Collections.emptyList();
		}
		Set<String> seenQueries = new LinkedHashSet<>(rawQueries.size());
		for (String rawQuery : rawQueries) {
			if (rawQuery == null) {
				continue;
			}
			String trimmedQuery = rawQuery.trim();
			if (!trimmedQuery.isEmpty()) {
				seenQueries.add(trimmedQuery);
			}
		}
		return new ArrayList<>(seenQueries);
	}

	public static List<String> parseNormalizedQueries(String queryString) {
		if (queryString == null || queryString.isEmpty()) {
			return Collections.emptyList();
		}
		Set<String> seenQueries = new LinkedHashSet<>(Math.max(8, queryString.length() / 16));
		int start = 0;
		int length = queryString.length();
		for (int i = 0; i < length; i++) {
			char ch = queryString.charAt(i);
			if (ch == '\n' || ch == '\r') {
				addNormalizedQuery(queryString, start, i, seenQueries);
				if (ch == '\r' && i + 1 < length && queryString.charAt(i + 1) == '\n') {
					i++;
				}
				start = i + 1;
			}
		}
		addNormalizedQuery(queryString, start, length, seenQueries);
		return new ArrayList<>(seenQueries);
	}

	private static void addNormalizedQuery(String queryString, int startInclusive, int endExclusive, Set<String> seenQueries) {
		if (startInclusive >= endExclusive) {
			return;
		}
		int lineStart = startInclusive;
		int lineEnd = endExclusive;
		while (lineStart < lineEnd && Character.isWhitespace(queryString.charAt(lineStart))) {
			lineStart++;
		}
		while (lineEnd > lineStart && Character.isWhitespace(queryString.charAt(lineEnd - 1))) {
			lineEnd--;
		}
		if (lineStart < lineEnd) {
			seenQueries.add(queryString.substring(lineStart, lineEnd));
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
		int qRunningTotal;       // unique (deduped) matched key count
		int grossMatchTotal;     // raw sum of per-query hit counts (before dedup)
		int totalRecordsProcessing;
		int completedRecordsSoFar;
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
