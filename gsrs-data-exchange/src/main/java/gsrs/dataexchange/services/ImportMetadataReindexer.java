package gsrs.dataexchange.services;

import gsrs.events.BeginReindexEvent;
import gsrs.events.EndReindexEvent;
import gsrs.events.IncrementReindexEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.repository.ImportMetadataRepository;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ImportMetadataReindexer {

    private static final boolean FORCE_SINGLE_THREADED_EVENT_HANDLING=false;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    ImportMetadataRepository importMetadataRepository;

    private final Map<UUID, CountDownLatch> latchMap = new ConcurrentHashMap<>();
    private final Map<UUID, TaskProgress> listenerMap = new ConcurrentHashMap<>();

    @Data
    @Builder
    private static class TaskProgress{
        private SchedulerPlugin.TaskListener listener;
        private UUID id;
        private long totalCount;
        private long currentCount;

        public synchronized void increment(){
            listener.message("Indexed: " + (++currentCount) + " of " + totalCount);
        }
    }
    @Async
    //@Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void executeAsync(Object id, SchedulerPlugin.TaskListener l) throws IOException {
        execute(id, l);
    }

    //@Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void execute(Object id, SchedulerPlugin.TaskListener l) throws IOException {
        l.message("Initializing reindexing");

        //this is all handled now by Spring events
        // TP 11/01/2021 TODO: this is used to lock how many events there should be,
        // but it's not clear this is actually the correct way for this to work
        // since the count of backup objects can change from this point
        // to the iteration. In addition, some objects can fail reindexing
        // for a variety of reasons. It's not clear to me why the count
        // mechanism should be used to track reindexing completeness
        // when the process isn't always for a known number of entities.
        // Worth reevaluating. For now, just making sure that even indexing failures
        // count as events.
        int count = (int) importMetadataRepository.count();
        Set<String> seen = Collections.newSetFromMap(new ConcurrentHashMap<>(count));
        log.debug("found count of " + count);

        UUID reindexId = (UUID) id;

        CountDownLatch endLatch = new CountDownLatch(1);
        latchMap.put(reindexId, endLatch);
        listenerMap.put(reindexId, TaskProgress.builder()
                .id(reindexId)
                .totalCount(count)
                .listener(l)
                .build());

        l.message("Initializing reindexing: acquiring list");

        LinkedBlockingDeque<Object> qevents = new LinkedBlockingDeque<>(1_000);

        Consumer<Object> eventConsumer;

        if(FORCE_SINGLE_THREADED_EVENT_HANDLING) {
            eventConsumer= (ev)->{
                qevents.add(ev);
            };
        }else {
            eventConsumer= (ev)->{
                log.trace("about to call eventPublisher.publishEvent");
                eventPublisher.publishEvent(ev);
            };
        }

        //This is hard-coded to 4 for now. It may be able to use the
        //common forkjoin pool, but there are reasons to suspect that
        //can overwhelm the application.
        ForkJoinPool customThreadPool = new ForkJoinPool(4);
        try {
            customThreadPool.submit(
                    () -> {
                        TransactionTemplate txRetrieveList = new TransactionTemplate(transactionManager);
                        txRetrieveList.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        txRetrieveList.setReadOnly(true);

                        List<ImportMetadata> blist=txRetrieveList.execute( t-> importMetadataRepository.findAll());
                        List<UUID> idList=txRetrieveList.execute( t->
                            importMetadataRepository.findAll().stream()
                                    .map(i->i.getRecordId())
                                    .collect(Collectors.toList())
                        );

                        TransactionTemplate tx = new TransactionTemplate(transactionManager);
                        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        tx.setReadOnly(true);
                        tx.executeWithoutResult(stat->{

                            log.trace("set reindexingCount to {}", idList.size());
                            eventConsumer.accept(new BeginReindexEvent(reindexId, idList.size(), BeginReindexEvent.IndexBehavior.WIPE_SPECIFIC_INDEX,
                                    Collections.singletonList(ImportMetadata.class)));
                            try(Stream<ImportMetadata> stream = blist.stream()){
                                l.message("Initializing reindexing: beginning process");

                                /*Stream<EntityUtils.EntityWrapper> ewStream=stream
                                        .map(oo->EntityUtils.EntityWrapper.of(oo));*/

                                Stream<UUID> uuidStream = idList.stream();
                                uuidStream
                                        .parallel()
                                        .forEach(wrapper ->{
                                            try {
                                                EntityFetcher<?> fetcher = EntityFetcher.of(EntityUtils.Key.of(ImportMetadata.class, wrapper));
                                                Optional<?> optionalImportMetadata= fetcher.getIfPossible();
                                                if(optionalImportMetadata.isPresent()){
                                                    EntityUtils.EntityWrapper<?> innerWrapper= EntityUtils.EntityWrapper.of(optionalImportMetadata.get());
                                                    innerWrapper.traverse().execute((p, child) -> {
                                                        log.trace("handling indexing of 'child' {}/{}", child.getKind(), child.getId());
                                                        //this should speed up indexing so that we only index
                                                        //things that are roots.  the actual indexing process of the root should handle any
                                                        //child objects of that root.
                                                        if (child.isEntity() && child.isRootIndex()) {
                                                            try {
                                                                log.trace("meets criteria for indexing");
                                                                EntityUtils.Key key = child.getKey();
                                                                String keyString = key.toString();

                                                                // TODO add only index if it has a controller?
                                                                // TP: actually, for subunits you need to index them even though there is no controller
                                                                // however, you could argue there SHOULD be a controller for them
                                                                if (seen.add(keyString)) {
                                                                    //indexOneItem(reindexId, eventConsumer, key, child);
                                                                    ReindexEntityEvent event = new ReindexEntityEvent(reindexId, key, Optional.of(child), false);
                                                                    eventConsumer.accept(event);
                                                                }
                                                            } catch (Throwable t) {
                                                                log.warn("indexing error handling:" + child, t);
                                                            }
                                                        }

                                                    });
                                                }
                                            }catch(Throwable ee) {
                                                log.warn("indexing error handling:" + wrapper, ee);
                                            }finally {
                                                log.trace("about to call eventConsumer.accept(new IncrementReindexEvent(reindexId))");
                                                eventConsumer.accept(new IncrementReindexEvent(reindexId));
                                            }
                                        });
                            }
                        });
                        return true;
                    });

            if(FORCE_SINGLE_THREADED_EVENT_HANDLING) {
                while(endLatch.getCount()>0) {
                    log.trace("endLatch.getCount(): {}", endLatch.getCount());
                    Object ot=qevents.take();
                    eventPublisher.publishEvent(ot);
                }
            }
        }catch(Exception e) {
            log.warn("indexing error", e);
            endLatch.countDown();
        }


        //other index listeners now figure out when indexing end is so don't need to that publish anymore (here)
        //but we will block until we get that end event
        try {
            endLatch.await();
        } catch (InterruptedException e) {
            log.warn("Reindexing interrupted", e);
        }

    }

    @EventListener(EndReindexEvent.class)
    public void endReindex(EndReindexEvent event){
        log.trace("in endReindex");
        CountDownLatch latch = latchMap.remove(event.getId());
        if(latch !=null){
            latch.countDown();
        }
    }

    @EventListener(IncrementReindexEvent.class)
    public void incrementReindexEvent(IncrementReindexEvent event){
        log.trace("in incrementReindexEvent");
        TaskProgress progress = listenerMap.get(event.getId());
        if(progress !=null){
            progress.increment();
        }
    }

    public static void indexOneItem(UUID reindexId, Consumer<Object> eventConsumer, EntityUtils.Key key,
                                    EntityUtils.EntityWrapper<ImportMetadata> wrappedEntity) {
        log.trace("indexOneItem will process reindex of key {}",  key);
        ReindexEntityEvent event = new ReindexEntityEvent(reindexId, key, Optional.of(wrappedEntity), false);
        eventConsumer.accept(event);
        log.trace("submitted index event");
    }
}
