package gsrs;


import ix.core.models.Edit;
import ix.core.util.EntityUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by katzelda on 4/18/17.
 */
@Slf4j
public class EditLock {


    private static class Counter{
        private int count=0;

        public void increment(){
            count++;
        }

        public int decrementAndGet(){
            return --count;
        }

        public int intValue(){
            return count;
        }
    }
    @Data
    public static class EditInfo{
        private Class<?> entityClass;
        private Object entityId;
        private String comments;
        private String oldJson;

        public static EditInfo from(EntityUtils.EntityWrapper<?> ew){
            String oldJSON = ew.toFullJson();
            EditLock.EditInfo editInfo = new EditLock.EditInfo();
            editInfo.setOldJson(oldJSON);
            editInfo.setEntityClass(ew.getEntityClass());
            editInfo.setEntityId(ew.getEntityInfo().getNativeIdFor(ew.getValue()).get());

            return editInfo;
        }
    }
    private Counter count = new Counter();
    private LockProxy lock = new LockProxy(new ReentrantLock());


    private final  Map<EntityUtils.Key, EditLock> lockMap;


    private EditInfo editInfo = null;


    private boolean preUpdateWasCalled = false;
    private boolean postUpdateWasCalled = false;

    private Runnable onPostUpdate = () -> {}; //no-op


    private final EntityUtils.Key thekey;

    public EditLock(EntityUtils.Key thekey, Map<EntityUtils.Key, EditLock> lockMap) {
        this.thekey = thekey;
        this.lockMap = lockMap;
    }



    public boolean tryLock() {
        return this.lock.tryLock();
    }

    public boolean hasEdit() {
        return this.editInfo != null;
    }

    public Optional<EditLock.EditInfo> getEdit() {
        return Optional.ofNullable(editInfo);
    }

    public EditLock addEdit(EditLock.EditInfo e) {
        if (hasEdit()) {
            log.warn("Existing edit will be overwritten");
        }
        this.editInfo = e;
        return this;
    }



    public void acquire() {
        synchronized (count) {
            count.increment();
        }
        
        while (true) {
            try {
                if (lock.tryLock(1, TimeUnit.SECONDS)) {
                    break;
                } else {
                    log.warn("still waiting for lock with key " + thekey);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if(count.intValue()>1){
        	log.warn(this.thekey + ": has more than 1 lock active:" + count.intValue());
        }

        //reset
        preUpdateWasCalled = false;
        postUpdateWasCalled = false;
        this.editInfo = null;

    }


    public EditLock addOnPostUpdate(Runnable r) {
        Runnable rold = this.onPostUpdate;
        this.onPostUpdate = ()->{
        	rold.run();
        	r.run();
        };
        return this;
    }

    public void release() {
        synchronized (count) {
            count.decrementAndGet();
        }
        try{
        	lock.forceUnlock();
        }catch(Exception e){
        	e.printStackTrace();
        	throw e;
        }
        synchronized (count) {
            int value = count.intValue();
            if (value == 0) {
                //no more blocking records?
                //remove ourselves from the map to free memory
            	lockMap.remove(thekey);
            }
        }
    }

    public void markPreUpdateCalled() {
        preUpdateWasCalled = true;
    }

    public void markPostUpdateCalled() {
        if (postUpdateWasCalled == false && this.onPostUpdate != null) {
            onPostUpdate.run();

        }
        postUpdateWasCalled = true;

    }

    public boolean hasPreUpdateBeenCalled() {
        return preUpdateWasCalled;
    }

    public boolean hasPostUpdateBeenCalled() {
        return postUpdateWasCalled;
    }


	public int getCount() {
		return count.intValue();
	}

    static class LockProxy implements Lock {

        // The actual lock.
        private volatile Lock lock;

        public LockProxy(Lock lock) {
            // Trap the lock we are proxying.
            this.lock = lock;
        }

        @Override
        public void lock() {
            // Proxy it.
            lock.lock();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            // Proxy it.
            lock.lockInterruptibly();
        }

        @Override
        public boolean tryLock() {
            // Proxy it.
            return lock.tryLock();
        }

        @Override
        public boolean tryLock(long l, TimeUnit tu) throws InterruptedException {
            // Proxy it.
            return lock.tryLock(l, tu);
        }

        @Override
        public void unlock() {
            // Proxy it.
            lock.unlock();
        }

        @Override
        public Condition newCondition() {
            // Proxy it.
            return lock.newCondition();
        }

        // Extra functionality to unlock from any thread.
        public void forceUnlock() {
            // Actually just replace the perhaps locked lock with a new one.
            // Kinda like a clone. I expect a neater way is around somewhere.
            if (lock instanceof ReentrantLock) {
                lock = new ReentrantLock();
            } else {
                throw new UnsupportedOperationException(
                        "Cannot force unlock of lock type "
                                + lock.getClass().getSimpleName());
            }
        }
    }
}
