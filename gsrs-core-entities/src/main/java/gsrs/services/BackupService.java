package gsrs.services;

import gov.nih.ncats.common.sneak.Sneak;
import gsrs.events.BackupEvent;
import gsrs.events.RemoveBackupEvent;
import gsrs.repository.BackupRepository;
import gsrs.repository.GsrsRepository;
import ix.core.models.BackupEntity;
import ix.core.models.FetchableEntity;
import ix.core.util.EntityUtils;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
public class BackupService {
    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private Map<UUID, CountDownLatch> latchMap = new ConcurrentHashMap<>();

    private Map<UUID, TaskProgress> taskProgressMap = new ConcurrentHashMap<>();
    @Data
    @Builder
    public static class TaskProgress{
        private Consumer<TaskProgress> listener;
        private UUID id;
        private long totalCount;
        private long currentCount;
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private CountDownLatch latch;

        public synchronized void increment(){
            currentCount++;
            if(listener !=null){
                listener.accept(this);
            }
            if(currentCount >=totalCount){
                latch.countDown();
            }
        }
    }
//    @Autowired
//    public BackupService(BackupRepository backupRepository, EntityManager em,
//                         ApplicationEventPublisher applicationEventPublisher){
//        this.backupRepository = backupRepository;
//        this.em = em;
//        this.applicationEventPublisher = applicationEventPublisher;
//    }


    @Transactional(readOnly = true)
    public void reBackupAllEntitiesOfType(GsrsRepository repository, Consumer<TaskProgress> listener){

        long total = repository.count();



        UUID rebackUpId = UUID.randomUUID();
        CountDownLatch latch = new CountDownLatch(1);
        latchMap.put(rebackUpId, latch);
        taskProgressMap.put(rebackUpId, TaskProgress.builder()
                .id(rebackUpId)
                .listener(listener)
                .totalCount(total)
                .latch(latch)
                .build());

        boolean[] wait = new boolean[1];
        try(Stream<Object> o = repository.findAll().stream()){

            o.forEach( e->{
                System.out.println("for each "+ e);
                wait[0]=true;
                backupIfNeededAsync(e, source->{
                    BackupEvent event = BackupEvent.builder()
                            .source(source)
                            .reBackupTaskId(rebackUpId)
                            .build();
                    System.out.println("publishing " + event);

                    try {
                        backup(event);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                });
            });

        }
        if(wait[0]) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //if we get this far we're done
        latchMap.remove(rebackUpId);
        taskProgressMap.remove(rebackUpId);
    }

    @Async
    public void backupIfNeededAsync(Object o, Consumer<BackupEntity> consumer){
        backupIfNeeded(o, consumer);
    }
    public void backupIfNeeded(Object o, Consumer<BackupEntity> consumer){
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(o);
        if(o instanceof FetchableEntity && ew.getEntityInfo().hasBackup()){
            try {
                BackupEntity be = new BackupEntity();
                be.setInstantiated((FetchableEntity) o);
                consumer.accept(be);
            } catch (Exception e) {
                Sneak.sneakyThrow(e);
            }


        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void backup(BackupEvent event) throws Exception {
        BackupEntity be = event.getSource();
        UUID rebackupId = event.getReBackupTaskId();
        if(rebackupId !=null){
            System.out.println("here!!!");
            TaskProgress progress = taskProgressMap.get(rebackupId);
            if(progress !=null) {
                progress.increment();
            }
        }
        Optional<BackupEntity> old = backupRepository.findByRefid(be.getRefid());
        if(old.isPresent()){
            BackupEntity updated= old.get();
            updated.setFromOther(be);
            backupRepository.saveAndFlush(updated);
        }else{
            backupRepository.saveAndFlush(be);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteBackup(RemoveBackupEvent event) throws Exception {
        Optional<BackupEntity> old = backupRepository.findByRefid(event.getRefid());
        if(old.isPresent()){
            backupRepository.delete(old.get());
        }
    }
}
