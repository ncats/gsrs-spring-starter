package gsrs;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import gov.nih.ncats.common.util.CachedSupplier;
import ix.utils.LiteralReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MixinUtil {
    private final static Map<LiteralReference<Object>,Map<String,Object>> _store = new ConcurrentHashMap<>();
    @SuppressWarnings("rawtypes")
    private final static ReferenceQueue refq = new ReferenceQueue<>();
    private final static ExecutorService executor = Executors.newFixedThreadPool(1);
    private final static CachedSupplier<AtomicBoolean> initializer = CachedSupplier.of(()->{
        AtomicBoolean isFlushing = new AtomicBoolean(true);
        executor.execute(()->{
            while(true) {
                try {
                    refq.remove();
                    while(refq.poll()!=null) {}
                } catch (InterruptedException e) {}
                clearAllStaleReference();
            }
        });
        return isFlushing;
    });
    
    private final static int MAX_CONCURRENT_READS = 10;
    private final static Semaphore semaphore = new Semaphore(MAX_CONCURRENT_READS);

    
    private static void clearAllStaleReference() {
        try {
            semaphore.acquireUninterruptibly(MAX_CONCURRENT_READS);
            int scount = _store.entrySet().size();
            boolean changed=false;
            Iterator<Entry<LiteralReference<Object>,Map<String,Object>>> es = _store.entrySet().iterator();
            while(es.hasNext()) {
                Entry<LiteralReference<Object>,Map<String,Object>> entry = es.next();
                LiteralReference<?> lit = entry.getKey();
                if(lit.isStale()) {
                    es.remove();
                    changed=true;
                }
            }
            if(changed) {
                log.debug("MixIn Store cleared from:" + scount + " to " + _store.size());
            }
        }finally {
            semaphore.release(MAX_CONCURRENT_READS);
        }
    }
    
    protected static Map<String,Object> getStore(Object o) {
        initializer.get();
        LiteralReference lr = LiteralReference.of(o,refq);
        try {
            semaphore.acquireUninterruptibly(1);
            return _store.computeIfAbsent(lr, k->{
                return new HashMap<String,Object>();
            });
        }finally {
            semaphore.release(1);
        }
    }
    
    protected static void clearStore(Object o) {
        initializer.get();
        LiteralReference lr = LiteralReference.of(o,refq);
        try {
            semaphore.acquireUninterruptibly(1);
            _store.remove(lr);
        }finally {
            semaphore.release(1);
        }
    }
    
    public static int getMixinStoreSize() {
        return _store.size();
    }
}
