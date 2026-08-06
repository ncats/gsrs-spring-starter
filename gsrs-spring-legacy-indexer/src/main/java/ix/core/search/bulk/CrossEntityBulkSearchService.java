package ix.core.search.bulk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
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

    private static class ServiceTarget {
        final String beanName;
        final LegacyGsrsSearchService service;

        ServiceTarget(String beanName, LegacyGsrsSearchService service) {
            this.beanName = beanName;
            this.service = service;
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
        CompletionService<SearchResultContext> completionService = new ExecutorCompletionService<>(getExecutor());
        List<Future<SearchResultContext>> futures = new ArrayList<>(selectedServices.size());
        for (ServiceTarget target : selectedServices) {
            futures.add(completionService.submit(serviceSearchTask(target, request, copyOptions(options))));
        }

        SearchResultContext merged = new SearchResultContext();
        merged.setKey(jobKey);
        merged.setStart(System.currentTimeMillis());

        Set<Key> seenRootKeys = new HashSet<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        int remaining = futures.size();
        while (remaining > 0) {
            long waitNanos = deadline - System.nanoTime();
            if (waitNanos <= 0) {
                cancelOutstanding(futures);
                log.warn("Cross-entity search timed out waiting for service results; cancelling remaining tasks");
                break;
            }

            Future<SearchResultContext> completed;
            try {
                completed = completionService.poll(waitNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancelOutstanding(futures);
                throw e;
            }
            if (completed == null) {
                cancelOutstanding(futures);
                log.warn("Cross-entity search timed out waiting for service results; cancelling remaining tasks");
                break;
            }

            remaining--;
            try {
                SearchResultContext ctx = completed.get();
                if (ctx != null && ctx.getResults() != null) {
                    processServiceContext(ctx, merged, seenRootKeys);
                }
            } catch (Exception e) {
                log.warn("Error retrieving search results from one service", e);
            }
        }

        merged.setTotal(merged.getCount());
        merged.setStatus(SearchResultContext.Status.Done);
        merged.setStop(System.currentTimeMillis());
        merged.setMessage("Cross-entity bulk search complete");
        return merged;
    }

    private void processServiceContext(SearchResultContext ctx, SearchResultContext merged,
                                       Set<Key> seenRootKeys) {
        if (ctx.getResults().isEmpty()) {
            return;
        }

        // Collect context updates locally to avoid interleaving reads and writes in the cache
        Map<Key, Map<String, Object>> pendingUpdates = new HashMap<>();

        for (Object result : ctx.getResults()) {
            if (!(result instanceof Key)) {
                continue;
            }

            Key key = (Key) result;
            Key rootKey = key.toRootKey();

            if (seenRootKeys.add(rootKey)) {
                merged.add(key);

                Map<String, Object> matching = cache.getMatchingContextByContextID(ctx.getId(), rootKey);
                Map<String, Object> contextData = matching != null
                        ? new HashMap<>(matching.size() + 1)
                        : new HashMap<>(1);
                if (matching != null) {
                    contextData.putAll(matching);
                }
                contextData.put("entityKind", key.getKind());
                pendingUpdates.put(rootKey, contextData);
            }
        }

        // Flush all collected updates in one pass
        for (Map.Entry<Key, Map<String, Object>> entry : pendingUpdates.entrySet()) {
            cache.setMatchingContext(merged.getId(), entry.getKey(), entry.getValue());
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

    private void cancelOutstanding(List<Future<SearchResultContext>> futures) {
        for (Future<SearchResultContext> future : futures) {
            future.cancel(true);
        }
    }

    private Callable<SearchResultContext> serviceSearchTask(ServiceTarget service,
                                                            BulkSearchService.SanitizedBulkSearchRequest request,
                                                            SearchOptions options) {
        return () -> {
            SearchResultContext ctx = service.service.bulkSearch(request, options);
            if (ctx != null) {
                try {
                    // Reduced timeout from 5 minutes to 30 seconds for better responsiveness
                    // on slow queries. Partial results are still returned if timeout occurs
                    ctx.getDeterminedFuture().get(30, TimeUnit.SECONDS);
                } catch (java.util.concurrent.TimeoutException e) {
                    // Log timeout but continue - we have partial results from this service
                    log.debug("Service {} bulk search timed out after 30s, returning partial results",
                              service.beanName);
                    // Don't fail the entire cross-entity search due to one slow service
                }
            }
            return ctx;
        };
    }

    private SearchOptions copyOptions(SearchOptions options) {
        SearchOptions copy = new SearchOptions();
        copy.parse(options.asQueryParams());
        return copy;
    }
}
