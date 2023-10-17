package gsrs;


import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import gov.nih.ncats.common.util.Unchecked;
import gsrs.events.CreateEditEvent;
import ix.core.models.Edit;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import lombok.extern.slf4j.Slf4j;
@Component
@Slf4j
public class EntityPersistAdapter {




    public static final String GSRS_HISTORY_DISABLED = "gsrs.history.disabled";

    // You need this Spring bean
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    // Do we need both?
    private Map<Key, EditLock> lockMap = new ConcurrentHashMap<>();


    public boolean isReindexing = false;

    @Autowired
    Environment env;

    private ConcurrentHashMap<Key, Integer> alreadyLoaded = new ConcurrentHashMap<>(10000);;

    /**
     * Preparing the edit ...
     * 
     * @param ew
     * @return
     */
    private EditLock.EditInfo createAndPushEditForWrappedEntity(EntityWrapper ew, EditLock lock) {
        Objects.requireNonNull(ew);
        EditLock.EditInfo editInfo =  EditLock.EditInfo.from(ew);

        lock.addEdit(editInfo);
        return editInfo;
    }

    /**
     * Used to apply the change operation during a locked edit. Empty optional
     * is used to cancel an operation, and a non-empty optional will be used
     * just to pass through.
     * 
     * @author peryeata
     *
     * @param <T>
     */
    public interface ChangeOperation<T> {
        Optional<?> apply(T obj) throws Exception; // Can't be of T type,
                                                   // unfortunately ... may
                                                   // return different thing
    }
    
    /**
     * This is the same as {@link #change(Key, ChangeOperation)}, except that it takes in the object to
     * be changed rather than the key to retrieve that object.
     * 
     * Note that the actual object will still be fetched using the key from the
     * database. This is because it may be stale at this point.
     * 
     * @param t
     * @param changeOp
     * @return
     */
    public <T> EntityWrapper<T> performChangeOn(T t, ChangeOperation<T> changeOp) {
        EntityWrapper<T> wrapped = EntityWrapper.of(t);
       
        return change(wrapped.getKey(), changeOp);
    }


    private <T> Optional<T> findByKey(EntityManager em, EntityUtils.Key key) {

        // The class of this key may not be the same
        // as the class it's stored in in the DB. Need to go
        // generic. The Key object should probably support
        // something like Key.toRootKey() or something to get
        // the more generic form. As-is this may come up again
        // in other contexts.
        
        Class<T> kls = (Class<T>) key.getEntityInfo()
                                     .getInherittedRootEntityInfo()
                                     .getEntityClass();
        Object id = key.getIdNative();

        return (Optional<T>) Optional.ofNullable(em.find(kls, id));
    }

    public Optional<EditLock.EditInfo> getEditFor(Key k){
    	EditLock lock = lockMap.get(k);
    	if(lock !=null){
    	    return lock.getEdit();
        }
    	return Optional.empty();

    }

    /**
     * Perform the following {@link ChangeOperation} to the given Entity referenced by its key.
     * If the Key references an entity that does not exist, then no changes are performed;
     * otherwise the changeop is invoked and the entity is changed as an edit.
     * 
     * This method is used to accomplish 3 important things:
     *   1. It establishes a lock on edits for a record, stopping 2 changes from happening simultaneously. Optimistic locks
     *      prevent the worst case scenarios of double editing even if this method isn't used.
     *   2. It stores the JSON of the record BEFORE any edits are done, which is useful for history information
     *      stored in the {@link Edit} entities.
     *   3. It fetches the most recent form of the record and peforms the mutating operation on it.
     * 
     * @param key  The Key to the entity to change; can not be null but may point to an entity that doesn't exist yet.
     * @param changeOp the {@link ChangeOperation} to perform; can not be null.
     * @param <T> the type of the entity.
     * @return the EntityWrapper of the updated entity or {@code null} if no change was performed.
     * @throws NullPointerException if any parameter is null.
     */
    public <T> EntityWrapper<T> change(Key keyAsIs, ChangeOperation<T> changeOp) {

        Objects.requireNonNull(keyAsIs);
        Objects.requireNonNull(changeOp);
        
        //Experimental
        Key key = keyAsIs.toRootKey();
        

        // This should work, but feels wrong
        EditLock lock = lockMap.computeIfAbsent(key, (k) -> new EditLock(k, lockMap));
        

        lock.acquire(); // acquire the lock (blocks)

        boolean worked = false;
        try {
            //Find current object from Key
            //we have to split this into 2 lines so Java 8 correct infers T
            
            Optional<T> opt = findByKey(key.getEntityManager(), key);
            //sometimes we ask for a key that doesn't exist yet.
            if(!opt.isPresent()){
                return null;
            }
            EntityWrapper<T> ew = EntityWrapper.of(opt.get()); // supplies
                                                                        // the
                                                                        // object
                                                                        // to be
                                                                        // edited,
            // you could have a different supplier
            // for this, but it's nice to be sure
            // that the object can't be stale




            createAndPushEditForWrappedEntity(ew, lock); // Doesn't block,
                                                             // or even check
                                                             // for
                                                             // existence of an
                                                             // active edit
                                                             // let's hope it
                                                             // works anyway!

            Optional op = changeOp.apply((T) ew.getValue()); // saving happens
                                                             // here
                                                             // So should
                                                             // anything with
                                                             // the edit
                                                             // inside of a post
                                                             // Update hook
            EntityWrapper saved = null;

            // didn't work, according to change operation
            // Either there was an error, or the decision
            // to change was cancelled
            if (!op.isPresent()) {
                return null;
            } else {
                saved = EntityWrapper.of(op.get());
            }
            //dkatzel 1/3/2018 - below edit block removed because we should now be making the edit upstream
          /*  e.kind = saved.getKind();
            e.newValue = saved.toFullJson();
            e.comments = ew.getChangeReason().orElse(null);
            e.save();*/
            worked = true;

            return saved;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
//            if (lock.getTransaction() == null) {
                lock.release(); // release the lock
//            }
        }
    }



    public <E extends Exception> boolean preUpdateBeanDirect(Object bean, Unchecked.ThrowingRunnable<E> runnable) throws E{
        EntityWrapper<?> ew = EntityWrapper.of(bean);
        Key key = ew.getKey().toRootKey();
        EditLock ml = lockMap.computeIfAbsent(key, (k) -> new EditLock(k, lockMap));
//        if (ml != null && ml.hasPreUpdateBeenCalled()) {
//            return true; // true?
//        }
//        if(ml.acquireIfFree()) {
//            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
//
//                @Override
//                public void afterCompletion(int status) {
//                    //this should be called if commit or rollback
//                    ml.release();
//                }
//            });
//        }

        runnable.run();

//        if (ml != null) {
//            ml.markPreUpdateCalled();
//        }

        return true;
    }

    public ThreadLocal<Boolean> historyEnabled = ThreadLocal.withInitial(()->true);

    // Pass a runnable to this method, and it will be run with historyEnabled=false.
    // Should only be used in threads that don't spawn other threads.
    // Useful for big bulk automated tasks where capturing edits is less important.
    public void runWithDisabledHistory(Runnable r) {

        this.historyEnabled.set(false);
        try {
            r.run();
        }finally {
            this.historyEnabled.set(true);
        }
    }

    public <E extends Exception> void postUpdateBeanDirect(Object bean, Object oldvalues, boolean storeEdit,  Unchecked.ThrowingRunnable<E> runnable) throws E{

        boolean shouldStore =  !env.getProperty(GSRS_HISTORY_DISABLED , Boolean.class,  false);

        if(!historyEnabled.get()) {
            shouldStore=false;
        }

        EntityWrapper<?> ew = EntityWrapper.of(bean);
        EditLock ml = lockMap.get(ew.getKey().toRootKey());

        if (ml != null && ml.hasPostUpdateBeenCalled()) {
            return;
        }
        if (ew.ignorePostUpdateHooks()) {
            return;
        }


        try {
            if (storeEdit && (ew.isEntity() && ew.storeHistory() && ew.hasKey() && shouldStore)) {

                // If we didn't already start an edit for this
                // then start one. Otherwise just ignore
                // the edit piece.
                // This section is for all of those entities that should have edits but which
                // are not updated via the EntityPersistAdapter#change(key,op) method. When edits
                // happen via the REST API, or via an intentional equivalent for root-level entities
                // they should always happen through the change operation. 
                if (ml == null || !ml.hasEdit()) {

                    EditLock.EditInfo editInfo = EditLock.EditInfo.from(ew);
                    editInfo.setOldJson(null);
                    if(editInfo.getVersion()!=null) {
                        try {
                            int iv=Integer.parseInt(editInfo.getVersion());
                            if(iv>=1) {
                                String inferredOldVersionNumber =((iv-1)+""); 
                                editInfo.setVersion(inferredOldVersionNumber);
                                log.debug("New version string [version="
                                        + editInfo.getVersion()
                                        + "] on auto-edited record [key="
                                        + ew.getKey()
                                        + "] was captured only after an edit was complete. This means the update was handled outside of a registered change. Changes to versioned entities should typically be registered before hand. Old version inferred to be [version = "
                                        + inferredOldVersionNumber + "]");
                            }
                        }catch(Exception e) {
                            log.warn("Version string [version=" + editInfo.getVersion() + "] on auto-edited record [key=" + ew.getKey() + "] is not an integer. It won't be properly represented as a version key.");
                        }
                    }
                    editInfo.setComments(ew.getChangeReason().orElse(null));
                    //                    entityManager.merge(edit);
                    ml.addEdit(editInfo);

                }else{
                    EditLock.EditInfo e = ml.getEdit().get();
                    e.setEntityClass(ew.getEntityClass());
                    if(ew.getChangeReason().isPresent()){

                        if(e.getComments() ==null){
                            e.setComments( ew.getChangeReason().get());
                        }else{
                            //append comment ?
                            e.setComments(e.getComments() + ew.getChangeReason().get());
                        }
                    }


//                    entityManager.merge(e);
                }
            }


        } catch (Exception ex) {
            log.trace("Can't retrieve bean id", ex);
        }

            runnable.run();
            //TODO remove from cache
//            IxCache.removeAllChildKeys(ew.getKey().toString());
            if (ml != null) {
                ml.markPostUpdateCalled();
                ml.getEdit().ifPresent(e->{
                    CreateEditEvent event = new CreateEditEvent();
                    
                    event.setComments(e.getComments());
                    event.setId(e.getEntityId());
                    event.setKind(e.getEntityClass());
                    event.setVersion(e.getVersion());
                    
                    // In 2.X 99.9% of the time we cared about an edit, we would have it 
                    // serialized from JSON via the "change" or "preformChangeOn" operations.
                    // For some reason, here, we still keep that code but then throw away that
                    // calculation.
                    event.setOldJson(e.getOldJson());

                    applicationEventPublisher.publishEvent(event);
                });
            }


    }







//
//    public void deepreindex(Object bean) {
//        deepreindex(bean, true);
//    }
//
//    public void deepreindex(Object bean, boolean deleteFirst) {
////        Java8ForOldEbeanHelper.deepreindex(this, EntityWrapper.of(bean), deleteFirst);
//        EntityWrapper.of(bean).traverse().execute((p, child)->reindex(child, deleteFirst));
//    }
//
//
//    public void reindex(Object bean) {
//        reindex(EntityWrapper.of(bean), true);
//    }
//
//    public void reindex(EntityWrapper ew, boolean deleteFirst) {
//
//        try {
//            if (ew.hasKey()) {
//                Key key = ew.getKey();
//                if (key != null) {
//                    // TODO: Investigate this
//                    if (isReindexing) {
//                        if (alreadyLoaded.containsKey(key)) {
//                            return;
//                        }
//                        alreadyLoaded.put(key, 0);
//                    }
//                }
//                if (deleteFirst) {
//                    deleteIndexOnBean(ew.getValue());
//                }
//                makeIndexOnBean(ew.getValue());
//            }
//        } catch (Exception e) {
//            log.error("Problem reindexing entity:", e);
//            e.printStackTrace();
//        }
//    }




}
