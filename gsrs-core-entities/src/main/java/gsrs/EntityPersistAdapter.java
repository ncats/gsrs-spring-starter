package gsrs;


import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.util.Unchecked;

import gsrs.model.AbstractGsrsEntity;
import ix.core.models.Edit;

import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.*;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
@Component
@Slf4j
public class EntityPersistAdapter {

    @PersistenceContext
    protected EntityManager entityManager;

    // You need this Spring bean
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    // Do we need both?
    private Map<Key, EditLock> lockMap = new ConcurrentHashMap<>();


    private Map<Key, String> workingOnJSON = new ConcurrentHashMap<>();

    public boolean isReindexing = false;

    @Autowired
    private OutsideTransactionUtil outsideTransactionUtil;

    private ConcurrentHashMap<Key, Integer> alreadyLoaded = new ConcurrentHashMap<>(10000);;

    /**
     * Preparing the edit ...
     * 
     * @param ew
     * @return
     */
    private Edit createAndPushEditForWrappedEntity(EntityWrapper ew, EditLock lock) {
        Objects.requireNonNull(ew);
        String oldJSON = ew.toFullJson();

        Edit e = new Edit(ew.getEntityClass(), ew.getKey().getIdString());
        e.oldValue = oldJSON;
        e.path = null;
        e.version = "unknown";


        if (ew.getVersion().isPresent()) {
            e.version = ew.getVersion().get().toString();
            // TODO: consider this
            // e.comments = e.version + " comment";
        }
        lock.addEdit(e);
        return e;
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
     * This is the same as performChange, except that it takes in the object to
     * be changed rather than the key to retrieve that object.
     * 
     * Note that the actual object will still be fetched using the key from the
     * database. This is because it may be stale at this point.
     * 
     * @param t
     * @param changeOp
     * @return
     */
    @Deprecated
    public <T> EntityWrapper performChangeOn(T t, ChangeOperation<T> changeOp) {
        EntityWrapper<T> wrapped = EntityWrapper.of(t);
        return performChange(wrapped.getKey(), changeOp);
    }

    @Deprecated
    public <T> EntityWrapper performChange(Key key, ChangeOperation<T> changeOp) {
        return change(key, changeOp);
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

    public Optional<Edit> getEditFor(Key k){
    	return Optional.ofNullable(lockMap.get(k))
    			       .map(new Function<EditLock, Edit>(){

							@Override
							public Edit apply(EditLock t) {
								return t.getEdit().orElse(null);
							}

    			       })
    			       .filter(new Predicate<Edit>(){

							@Override
							public boolean test(Edit t) {
								return t!=null;
							}

    			       });

    }

    public <T> EntityWrapper change(Key key, ChangeOperation<T> changeOp) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(changeOp);

        EditLock lock = lockMap.computeIfAbsent(key, new Function<Key, EditLock>() {
            @Override
            public EditLock apply(Key key) {
                return new EditLock(key, lockMap); // This should work, but
                                                   // feels wrong
            }
        });

        Edit e = null;
        lock.acquire(); // acquire the lock (blocks)

        boolean worked = false;
        try {
            //we have to split this into 2 lines so Java 8 correct infers T
            Optional<T> opt = findByKey(entityManager, key);

            EntityWrapper<T> ew = EntityWrapper.of(opt.get()); // supplies
                                                                        // the
                                                                        // object
                                                                        // to be
                                                                        // edited,
            // you could have a different supplier
            // for this, but it's nice to be sure
            // that the object can't be stale



            e = createAndPushEditForWrappedEntity(ew, lock); // Doesn't block,
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
            ex.printStackTrace();
            throw new IllegalStateException(ex);
        } finally {
//            if (lock.getTransaction() == null) {
                lock.release(); // release the lock
//            }
            workingOnJSON.remove(key);
        }
    }



    public <E extends Exception> boolean preUpdateBeanDirect(Object bean, Unchecked.ThrowingRunnable<E> runnable) throws E{
        EntityWrapper<?> ew = EntityWrapper.of(bean);
        Key key = ew.getKey();
        EditLock ml = lockMap.computeIfAbsent(key, new Function<Key, EditLock>() {
            @Override
            public EditLock apply(Key key) {
                return new EditLock(key, lockMap); // This should work, but
                // feels wrong
            }
        });
        if (ml != null && ml.hasPreUpdateBeenCalled()) {
            return true; // true?
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter(){

            @Override
            public void afterCompletion(int status) {
                //this should be called if commit or rollback
                ml.release();
            }
        });


        runnable.run();

        if (ml != null) {
            ml.markPreUpdateCalled();
        }

        return true;
    }




    public <E extends Exception> void postUpdateBeanDirect(Object bean, Object oldvalues, boolean storeEdit,  Unchecked.ThrowingRunnable<E> runnable) throws E{

        EntityWrapper<?> ew = EntityWrapper.of(bean);
        EditLock ml = lockMap.get(ew.getKey());

        if (ml != null && ml.hasPostUpdateBeenCalled()) {
            return;
        }
        if (ew.ignorePostUpdateHooks()) {
            return;
        }
        try {
            if (storeEdit && (ew.isEntity() && ew.storeHistory() && ew.hasKey())) {
                Key key = ew.getKey();
                // If we didn't already start an edit for this
                // then start one and save it. Otherwise just ignore
                // the edit piece.
                if (ml == null || !ml.hasEdit()) {

                    Edit edit = new Edit(ew.getEntityClass(), key.getIdString());
                    if(bean instanceof AbstractGsrsEntity){
                        AbstractGsrsEntity gsrsEntity = (AbstractGsrsEntity)bean;
                        edit.version = gsrsEntity.getPreviousVersion();
                        edit.oldValue = gsrsEntity.getPreviousState() ==null? null: gsrsEntity.getPreviousState().toString();
                    }

                    edit.comments = ew.getChangeReason().orElse(null);
                    edit.kind = ew.getKind();
                    edit.newValue = ew.toFullJson();

                    entityManager.persist(edit);

                }else{
                    Edit e = ml.getEdit().get();
                    e.kind = ew.getKind();
                    e.newValue = ew.toFullJson();
                    if(ew.getChangeReason().isPresent()){

                        if(e.comments ==null){
                            e.comments = ew.getChangeReason().get();
                        }else{
                            //append comment ?
                            e.comments += ew.getChangeReason().get();
                        }
                    }


                    entityManager.merge(e);
                }
            } else {
                log.warn("Entity bean [" + ew.getKind() + "]" + " doesn't have Id annotation!");
            }


        } catch (Exception ex) {
            log.trace("Can't retrieve bean id", ex);
        }

            runnable.run();
            //TODO remove from cache
//            IxCache.removeAllChildKeys(ew.getKey().toString());
            if (ml != null) {
                ml.markPostUpdateCalled();
            }


    }








//    public void deepreindex(Object bean) {
//        deepreindex(bean, true);
//    }
//
//    public void deepreindex(Object bean, boolean deleteFirst) {
//        Java8ForOldEbeanHelper.deepreindex(this, EntityWrapper.of(bean), deleteFirst);
//    }
//
//    public void reindex(Object bean) {
//        reindex(EntityWrapper.of(bean), true);
//    }

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
//            Logger.error("Problem reindexing entity:", e);
//            e.printStackTrace();
//        }
//    }




}
