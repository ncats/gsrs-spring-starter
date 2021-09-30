package gsrs.legacy;

import gsrs.events.BeginReindexEvent;
import gsrs.events.EndReindexEvent;
import gsrs.events.IncrementReindexEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.repository.BackupRepository;
import gsrs.util.TaskListener;
import ix.core.models.BackupEntity;
import ix.core.util.EntityUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class DefaultReindexService<T> implements ReindexService<T>{
    private final JpaRepository repository;

    private final List<Class> classesToIndex;
    private final List<String> classesToIndexAsStrings;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private Map<UUID, CountDownLatch> latchMap = new ConcurrentHashMap<>();
    private Map<UUID, TaskProgress> listenerMap = new ConcurrentHashMap<>();

    @Data
    @Builder
    private static class TaskProgress{
        private TaskListener listener;
        private UUID id;
        private long totalCount;
        private long currentCount;

        public synchronized void increment(){
            listener.message("Indexed:" + (++currentCount) + "of " + totalCount);
        }
    }

    public DefaultReindexService(JpaRepository<T, ?> repository, Class...classesToIndex) {
        this.repository = repository;
        this.classesToIndex = Arrays.asList(classesToIndex);
        this.classesToIndexAsStrings = this.classesToIndex.stream().map(Class::getName).collect(Collectors.toList());

    }
    @Async
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void executeAsync(Object id, TaskListener l, boolean wipeIndexFirst) throws IOException {
        execute( id, l, wipeIndexFirst);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void execute(Object id, TaskListener l, boolean wipeIndexFirst) throws IOException {
        l.message("Initializing reindexing");

//this is all handled now by Spring events
        int count = (int) repository.count();
        Set<String> seen = Collections.newSetFromMap(new ConcurrentHashMap<>(count));
        log.debug("found count of " + count);
        //single thread for now...
        UUID reindexId = (UUID) id;
        CountDownLatch latch = new CountDownLatch(1);
        latchMap.put(reindexId, latch);
        listenerMap.put(reindexId, TaskProgress.builder()
                .id(reindexId)
                .totalCount(count)
                .listener(l)
                .build());

        BeginReindexEvent.BeginReindexEventBuilder builder = BeginReindexEvent.builder()
                .id(reindexId)
                .numberOfExpectedRecord(count);
        if(wipeIndexFirst){
            builder.wipeIndexStrategy(BeginReindexEvent.WipeIndexStrategy.CLEAR_ALL);
        }else{
            builder.wipeIndexStrategy(BeginReindexEvent.WipeIndexStrategy.REMOVE_ONLY_SPECIFIED_TYPES)
                    .typesToClearFromIndex(classesToIndexAsStrings);
        }
        eventPublisher.publishEvent(builder.build());

        try(Stream<T> stream = repository.findAll().stream()){

            stream.forEach(be ->{
                try {


                        EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(be);

                        wrapper.traverse().execute((p, child) -> {
                            EntityUtils.EntityWrapper<EntityUtils.EntityWrapper> wrapped = EntityUtils.EntityWrapper.of(child);
                            //this should speed up indexing so that we only index
                            //things that are roots.  the actual indexing process of the root should handle any
                            //child objects of that root.
                            if (wrapped.isEntity() && wrapped.isRootIndex()) {
                                try {
                                    EntityUtils.Key key = wrapped.getKey();
                                    String keyString = key.toString();

                                    // TODO add only index if it has a controller?
                                    // TP: actually, for subunits you need to index them even though there is no controller
                                    // however, you could argue there SHOULD be a controller for them
                                    if (seen.add(keyString)) {
                                        //is this a good idea ?
                                        ReindexEntityEvent event = new ReindexEntityEvent(reindexId, key);
                                        eventPublisher.publishEvent(event);
                                    }
                                } catch (Throwable t) {
                                    log.warn("indexing error handling:" + wrapped, t);
                                }
                            }

                        });

                    eventPublisher.publishEvent(new IncrementReindexEvent(reindexId));
                } catch (Exception e) {
                    log.warn("indexing error handling:" + be, e);
                }
            });
        }
        //other index listeners now figure out when indexing end is so don't need to that publish anymore (here)
        //but we will block until we get that end event
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @EventListener(EndReindexEvent.class)
    public void endReindex(EndReindexEvent event){
        CountDownLatch latch = latchMap.remove(event.getId());
        if(latch !=null){
            latch.countDown();
        }
    }

    @EventListener(IncrementReindexEvent.class)
    public void endReindex(IncrementReindexEvent event){
        TaskProgress progress = listenerMap.get(event.getId());
        if(progress !=null){
            progress.increment();
        }
    }
}
