package ix.core.search.bulk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import gsrs.cache.GsrsCache;
import gsrs.legacy.LegacyGsrsSearchService;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResultContext;
import ix.core.util.EntityUtils.Key;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CrossEntityBulkSearchService {
    private static final long CROSS_ENTITY_PROGRESS_UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(250);
    private static final long SERVICE_SUMMARY_REFRESH_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(300);

    private static class ServiceTarget {
        final String beanName;
        final LegacyGsrsSearchService service;

        ServiceTarget(String beanName, LegacyGsrsSearchService service) {
            this.beanName = beanName;
            this.service = service;
        }
    }

    private static class ServiceSearchHandle {
        final String beanName;
        final SearchResultContext context;
        final Future<Void> determinedFuture;
        final String summaryCacheKey;
        int lastKnownRunningTotal;
        int lastKnownCompletedQueries;
        long nextSummaryRefreshNanos;
        boolean processed;

        ServiceSearchHandle(String beanName, SearchResultContext context, Future<Void> determinedFuture) {
            this.beanName = beanName;
            this.context = context;
            this.determinedFuture = determinedFuture;
            this.summaryCacheKey = context == null || context.getKey() == null ? null : "BulkSearchSummary/" + context.getKey();
            this.lastKnownRunningTotal = 0;
            this.lastKnownCompletedQueries = 0;
            this.nextSummaryRefreshNanos = 0L;
            this.processed = false;
        }
    }

    private static class ProgressTotals {
        final int runningTotal;
        final int completedQueries;

        ProgressTotals(int runningTotal, int completedQueries) {
            this.runningTotal = runningTotal;
            this.completedQueries = completedQueries;
        }
    }

    private final ApplicationContext applicationContext;
    private final GsrsCache cache;
    private final ExecutorService executor;

    public CrossEntityBulkSearchService(ApplicationContext applicationContext, GsrsCache cache) {
        this(applicationContext, cache,
                Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2)));
    }

    public CrossEntityBulkSearchService(ApplicationContext applicationContext, GsrsCache cache, ExecutorService executor) {
        this.applicationContext = applicationContext;
        this.cache = cache;
        this.executor = executor;
    }

    public SearchResultContext search(BulkSearchService.SanitizedBulkSearchRequest request,
                                      SearchOptions options,
                                      Collection<String> entityContexts) throws IOException {
        if (request == null || request.getQueries() == null || request.getQueries().isEmpty()) {
            throw new IOException("Bulk search request must contain at least one query");
        }

        Map<String, LegacyGsrsSearchService> services = getLegacySearchServices();
        List<ServiceTarget> selectedServices = selectServices(services, entityContexts);
        if (selectedServices.isEmpty()) {
            throw new IOException("No legacy search services available for cross-entity bulk search");
        }

        BulkSearchService.SanitizedBulkSearchRequest normalizedRequest = normalizeRequest(request);
        String serviceKey = buildServiceKey(selectedServices);
        String jobKey = normalizedRequest.computeKey(options.getBulkSearchOnIdentifiers(), options.getFacets())
                + "/" + serviceKey;

        long maxEpoch = 0L;
        for (ServiceTarget target : selectedServices) {
            Long lastModified = target.service.getLastModified();
            if (lastModified != null && lastModified.longValue() > maxEpoch) {
                maxEpoch = lastModified.longValue();
            }
        }

        try {
            return cache.getOrElse(maxEpoch, jobKey, () -> buildMergedContext(normalizedRequest, options, selectedServices, jobKey));
        } catch (Exception e) {
            throw new IOException("error performing cross-entity bulk search", e);
        }
    }

    protected Map<String, LegacyGsrsSearchService> getLegacySearchServices() {
        return applicationContext.getBeansOfType(LegacyGsrsSearchService.class);
    }

    protected ExecutorService getExecutor() {
        return executor;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    private List<ServiceTarget> selectServices(Map<String, LegacyGsrsSearchService> services,
                                               Collection<String> entityContexts) {
        if (entityContexts == null || entityContexts.isEmpty()) {
            List<ServiceTarget> selected = new ArrayList<>(services.size());
            for (Map.Entry<String, LegacyGsrsSearchService> entry : services.entrySet()) {
                selected.add(new ServiceTarget(entry.getKey(), entry.getValue()));
            }
            return selected;
        }

        Set<String> allowed = new HashSet<>();
        for (String entityContext : entityContexts) {
            if (entityContext == null) {
                continue;
            }
            String trimmed = entityContext.trim();
            if (!trimmed.isEmpty()) {
                allowed.add(trimmed);
            }
        }

        List<ServiceTarget> selected = new ArrayList<>(services.size());
        for (Map.Entry<String, LegacyGsrsSearchService> entry : services.entrySet()) {
            if (allowed.contains(entry.getKey())
                    || allowed.contains(entry.getValue().getClass().getSimpleName())) {
                selected.add(new ServiceTarget(entry.getKey(), entry.getValue()));
            }
        }
        return selected;
    }

    private SearchResultContext buildMergedContext(BulkSearchService.SanitizedBulkSearchRequest request,
                                                   SearchOptions options,
                                                   List<ServiceTarget> selectedServices,
                                                   String jobKey) throws Exception {
        CompletionService<ServiceSearchHandle> completionService = new ExecutorCompletionService<>(getExecutor());
        List<Future<ServiceSearchHandle>> submitFutures = new ArrayList<>(selectedServices.size());
        for (ServiceTarget target : selectedServices) {
            submitFutures.add(completionService.submit(serviceSearchTask(target, request, copyOptions(options))));
        }

        SearchResultContext merged = new SearchResultContext();
        merged.setKey(jobKey);
        merged.setStart(System.currentTimeMillis());
        merged.setStatus(SearchResultContext.Status.Running);
        merged.setRunningBulkSearchTotal(0);
        persistCrossEntitySummary(jobKey, options, request.getQueries().size(), selectedServices.size(), 0, 0);

        Set<Key> seenRootKeys = new HashSet<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        List<ServiceSearchHandle> handles = new ArrayList<>(selectedServices.size());
        List<ServiceSearchHandle> allHandles = new ArrayList<>(selectedServices.size());
        int lastPersistedRunningTotal = -1;
        int lastPersistedCompletedQueries = -1;
        int completedServiceCount = 0;
        long nextProgressUpdateNanos = System.nanoTime();
        int awaitingHandles = submitFutures.size();
        while (awaitingHandles > 0) {
            long waitNanos = deadline - System.nanoTime();
            if (waitNanos <= 0) {
                cancelOutstanding(submitFutures);
                break;
            }
            Future<ServiceSearchHandle> completedHandle;
            try {
                completedHandle = completionService.poll(waitNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancelOutstanding(submitFutures);
                throw e;
            }
            if (completedHandle == null) {
                cancelOutstanding(submitFutures);
                break;
            }
            awaitingHandles--;
            try {
                ServiceSearchHandle handle = completedHandle.get();
                if (handle != null && handle.context != null) {
                    handles.add(handle);
                    allHandles.add(handle);
                }
            } catch (Exception e) {
                log.warn("Error starting one cross-entity bulk search task", e);
            }
        }

        while (!handles.isEmpty()) {
            long waitNanos = deadline - System.nanoTime();
            if (waitNanos <= 0) {
                break;
            }

            Iterator<ServiceSearchHandle> handleIterator = handles.iterator();
            while (handleIterator.hasNext()) {
                ServiceSearchHandle handle = handleIterator.next();
                if (isServiceDetermined(handle)) {
                    processServiceIfNeeded(handle, merged, seenRootKeys);
                    completedServiceCount++;
                    handleIterator.remove();
                }
            }
            if (handles.isEmpty()) {
                break;
            }

            long now = System.nanoTime();
            if (now >= nextProgressUpdateNanos) {
                ProgressTotals totals = computeProgressTotals(handles, request.getQueries().size(), merged.getCount(), completedServiceCount);
                merged.setRunningBulkSearchTotal(totals.runningTotal);
                if (totals.runningTotal != lastPersistedRunningTotal
                        || totals.completedQueries != lastPersistedCompletedQueries) {
                    persistCrossEntitySummary(jobKey, options, request.getQueries().size(), selectedServices.size(),
                            totals.runningTotal, totals.completedQueries);
                    lastPersistedRunningTotal = totals.runningTotal;
                    lastPersistedCompletedQueries = totals.completedQueries;
                }
                nextProgressUpdateNanos = now + CROSS_ENTITY_PROGRESS_UPDATE_INTERVAL_NANOS;
            }

            try {
                TimeUnit.MILLISECONDS.sleep(Math.min(200L, Math.max(10L, TimeUnit.NANOSECONDS.toMillis(waitNanos))));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }

        for (ServiceSearchHandle handle : handles) {
            processServiceIfNeeded(handle, merged, seenRootKeys);
            if (isServiceDetermined(handle)) {
                completedServiceCount++;
            }
        }
        int finalCompletedQueries = request.getQueries().size() * selectedServices.size();
        merged.setTotal(merged.getCount());
        merged.setRunningBulkSearchTotal(merged.getCount());
        persistCrossEntitySummary(jobKey, options, request.getQueries().size(), selectedServices.size(), merged.getCount(),
                finalCompletedQueries);
        merged.setStatus(SearchResultContext.Status.Done);
        merged.setStop(System.currentTimeMillis());
        merged.setMessage("Cross-entity bulk search complete");
        return merged;
    }

    private ProgressTotals computeProgressTotals(List<ServiceSearchHandle> handles,
                                                 int queryCount,
                                                 int committedRunningTotal,
                                                 int completedServiceCount) {
        int runningTotal = committedRunningTotal;
        int maxPerServiceCompleted = Math.max(1, queryCount);
        int completedQueries = completedServiceCount * maxPerServiceCompleted;
        long nowNanos = System.nanoTime();
        for (ServiceSearchHandle handle : handles) {
            if (handle.summaryCacheKey != null && nowNanos >= handle.nextSummaryRefreshNanos) {
                BulkSearchService.BulkQuerySummary summary = getBulkSummaryForHandle(handle);
                if (summary != null) {
                    handle.lastKnownRunningTotal = Math.max(0, summary.getQRunningTotal());
                    handle.lastKnownCompletedQueries = Math.max(0, summary.getQCompleted());
                } else {
                    handle.lastKnownRunningTotal = Math.max(handle.lastKnownRunningTotal, handle.context.getCount());
                    if (isServiceDetermined(handle)) {
                        handle.lastKnownCompletedQueries = maxPerServiceCompleted;
                    }
                }
                handle.nextSummaryRefreshNanos = nowNanos + SERVICE_SUMMARY_REFRESH_INTERVAL_NANOS;
            }

            if (handle.summaryCacheKey != null) {
                runningTotal += handle.lastKnownRunningTotal;
                completedQueries += Math.min(maxPerServiceCompleted, Math.max(0, handle.lastKnownCompletedQueries));
                continue;
            }

            runningTotal += handle.context.getCount();
            if (isServiceDetermined(handle)) {
                completedQueries += maxPerServiceCompleted;
            }
        }
        return new ProgressTotals(runningTotal, completedQueries);
    }

    private void processServiceIfNeeded(ServiceSearchHandle handle,
                                        SearchResultContext merged,
                                        Set<Key> seenRootKeys) {
        if (handle.processed) {
            return;
        }
        SearchResultContext ctx = handle.context;
        if (ctx != null && ctx.getResults() != null) {
            processServiceContext(ctx, merged, seenRootKeys);
        }
        handle.processed = true;
    }

    private boolean isServiceDetermined(ServiceSearchHandle handle) {
        return (handle.determinedFuture != null && handle.determinedFuture.isDone())
                || (handle.context != null && handle.context.isDetermined());
    }

    private void persistCrossEntitySummary(String jobKey,
                                           SearchOptions options,
                                           int queryCount,
                                           int serviceCount,
                                           int runningTotal,
                                           int completedQueries) {
        int qTotal = Math.max(0, queryCount * serviceCount);
        BulkSearchService.BulkQuerySummary summary = BulkSearchService.BulkQuerySummary.builder()
                .qTotal(qTotal)
                .qTop(0)
                .qSkip(0)
                .qMatchTotal(runningTotal)
                .qUnMatchTotal(0)
                .qCompleted(Math.min(qTotal, Math.max(0, completedQueries)))
                .qRunningTotal(Math.max(0, runningTotal))
                .qFilteredTotal(Math.min(qTotal, Math.max(0, completedQueries)))
                .qFilter(options.getFilter())
                .qSort(options.getOrder() != null ? String.join(",", options.getOrder()) : null)
                .searchOnIdentifiers(options.getBulkSearchOnIdentifiers())
                .facets(options.getFacets() == null ? Collections.emptyList() : new ArrayList<>(options.getFacets()))
                .queries(Collections.emptyList())
                .build();
        cache.setRaw("BulkSearchSummary/" + jobKey, summary);
    }

    private BulkSearchService.BulkQuerySummary getBulkSummaryForHandle(ServiceSearchHandle handle) {
        if (handle == null || handle.summaryCacheKey == null) {
            return null;
        }
        Object cached = cache.getRaw(handle.summaryCacheKey);
        if (cached instanceof BulkSearchService.BulkQuerySummary) {
            return (BulkSearchService.BulkQuerySummary) cached;
        }
        return null;
    }

    private void processServiceContext(SearchResultContext ctx, SearchResultContext merged,
                                       Set<Key> seenRootKeys) {
        if (ctx.getResults().isEmpty()) {
            return;
        }

        for (Object result : ctx.getResults()) {
            if (!(result instanceof Key)) {
                continue;
            }

            Key key = (Key) result;
            Key rootKey = key.toRootKey();

            if (seenRootKeys.add(rootKey)) {
                merged.add(key);

                Map<String, Object> matching = cache.getMatchingContextByContextID(ctx.getId(), rootKey);
                Map<String, Object> contextData = matching != null ? matching : new HashMap<>(2);
                contextData.put("entityKind", key.getKind());
                cache.setMatchingContext(merged.getId(), rootKey, contextData);
            }
        }
    }

    private BulkSearchService.SanitizedBulkSearchRequest normalizeRequest(BulkSearchService.SanitizedBulkSearchRequest request) {
        Set<String> normalizedQueries = new LinkedHashSet<>(request.getQueries().size());
        for (String rawQuery : request.getQueries()) {
            if (rawQuery == null) {
                continue;
            }
            String trimmedQuery = rawQuery.trim();
            if (!trimmedQuery.isEmpty()) {
                normalizedQueries.add(trimmedQuery);
            }
        }

        BulkSearchService.SanitizedBulkSearchRequest normalizedRequest = new BulkSearchService.SanitizedBulkSearchRequest();
        normalizedRequest.setQueries(new ArrayList<>(normalizedQueries));
        return normalizedRequest;
    }

    private String buildServiceKey(List<ServiceTarget> selectedServices) {
        List<String> names = new ArrayList<>(selectedServices.size());
        for (ServiceTarget target : selectedServices) {
            names.add(target.beanName);
        }
        names.sort(String::compareTo);

        StringBuilder serviceKey = new StringBuilder();
        for (String name : names) {
            if (serviceKey.length() > 0) {
                serviceKey.append(',');
            }
            serviceKey.append(name);
        }
        return serviceKey.toString();
    }

    private <T> void cancelOutstanding(List<Future<T>> futures) {
        for (Future<T> future : futures) {
            future.cancel(true);
        }
    }

    private Callable<ServiceSearchHandle> serviceSearchTask(ServiceTarget service,
                                                            BulkSearchService.SanitizedBulkSearchRequest request,
                                                            SearchOptions options) {
        return () -> {
            SearchResultContext ctx = service.service.bulkSearch(request, options);
            return new ServiceSearchHandle(service.beanName, ctx, ctx == null ? null : ctx.getDeterminedFuture());
        };
    }

    private SearchOptions copyOptions(SearchOptions options) {
        SearchOptions copy = new SearchOptions();
        copy.parse(options.asQueryParams());
        return copy;
    }
}
