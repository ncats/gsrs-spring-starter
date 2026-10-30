package gsrs.services;

import gov.nih.ncats.common.sneak.Sneak;
import gsrs.events.BackupEvent;
import gsrs.events.RemoveBackupEvent;
import gsrs.repository.BackupRepository;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.models.BackupEntity;
import ix.core.models.FetchableEntity;
import ix.core.util.EntityUtils;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
public class BackupService {
    @Autowired
    private BackupRepository backupRepository;

    private Map<UUID, CountDownLatch> latchMap = new ConcurrentHashMap<>();

    private Map<UUID, TaskProgress> taskProgressMap = new ConcurrentHashMap<>();

    /**
     * Holds the progress of a given Backup Task.
     */
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

        /**
         * Increment the number of records processed by 1.
         */
        public synchronized void increment(){
            currentCount++;
            if(listener !=null){
                listener.accept(this);
            }
            if(latch !=null && currentCount >=totalCount){
                latch.countDown();
            }
        }
    }

    /**
     * Run through all the entities in the given repository fetching by Pageable sizes
     * and save all the backup-able records as new Backups in the Backup table replacing
     * and previous backup that previously existed.
     * @param repository the repository to query (can not be null).
     * @param pageable the Pageable size used to fetch records by this pageable size.
     * @param listener for each record that is successfully backed up this listener
     *                 will be consumed.  This is usually used to update some kind of progress bar.
     *
     * @throws NullPointerException if    repository or pageable are null.
     * @implSpec This is annotated as Transactional read only because this thread is doing only a read
     * of the repository, the actual saving in the backup table is done in other background threads.
     */
    @Transactional(readOnly = true)
    public void reBackupAllEntitiesOfType(JpaRepository repository, Pageable pageable,  Consumer<TaskProgress> listener){

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
        Pageable currentPageable = pageable;

        while(currentPageable !=null){
            Page page = repository.findAll(currentPageable);
            try(Stream<Object> o = page.stream()){

                o.forEach( e->{
                    wait[0]=true;

                    backupIfNeeded(e, source->{
                        BackupEvent event = BackupEvent.builder()
                                .source(source)
                                .reBackupTaskId(rebackUpId)
                                .build();
                        //this is a terrible hack to get the bean reference for AOP so we make
                        //new transaction boundaries even though we are calling methods from this class.
                        BackupService backupBean = StaticContextAccessor.getBean(BackupService.class);


                        try {
                            backupBean.backupAsync(event);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }

                    });
                });

            }
            if(page.hasNext()){
                currentPageable = page.nextPageable();
            }else{
                currentPageable=null;
            }
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void backupAsync(BackupEvent event) throws Exception {
        backup(event);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void backup(BackupEvent event) throws Exception {
        BackupEntity be = event.getSource();
        UUID rebackupId = event.getReBackupTaskId();
        if(rebackupId !=null){
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
