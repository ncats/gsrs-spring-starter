package gsrs.events.listeners;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import gsrs.events.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import gov.nih.ncats.common.util.SingleThreadCounter;
import gov.nih.ncats.common.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens for reindex events {@link BeginReindexEvent} and counts
 * of each {@link ReindexEntityEvent} to figure out when {@link EndReindexEvent} and
 * {@link MaintenanceModeEvent} should fire fired (and then publishes those events).
 */
@Service
@Slf4j
public class ReindexEventListener {
    //because our event listeners have to do multiple operations
    //on all of our fields in each method we synchronize on the method level
    //so we don't need to use AtomicBoolean or ConcurrentHashMaps because we get no performance improvement from them.
    private Map<UUID, SingleThreadCounter> reindexCounts = new HashMap<>();
    private boolean inMaintenanceMode = false;

    private Map<UUID, Long> reindexTimes = new HashMap<>();
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    public synchronized void onNewReindex(BeginReindexEvent event){
        long expected=event.getNumberOfExpectedRecord();
        reindexCounts.put(event.getId(), new SingleThreadCounter(expected));
        reindexTimes.put(event.getId(), TimeUtil.getCurrentTimeMillis());

        //todo: extract the concept of MaintenanceMode from the index maintenance
        if(event.getIndexBehavior()== BeginReindexEvent.IndexBehavior.WIPE_ALL_INDEXES && !inMaintenanceMode){
            inMaintenanceMode=true;
            //was not in maintenanceMode mode before and now it is
            applicationEventPublisher.publishEvent(new MaintenanceModeEvent(MaintenanceModeEvent.Mode.BEGIN));
        }

        if( event.getIndexBehavior() == BeginReindexEvent.IndexBehavior.WIPE_SPECIFIC_INDEX){
            //iterate through classes
            event.getClassesToRemoveFromIndex().forEach(c->{
                applicationEventPublisher.publishEvent(new ClearIndexByTypeEvent(c));
            });
        }
        if (expected == 0L) {
            finishReindexEvent(event.getId());
        }
        
    }
    
    private void finishReindexEvent(UUID id) {
        reindexCounts.remove(id);
        applicationEventPublisher.publishEvent(new EndReindexEvent(id));
        if (reindexCounts.isEmpty()) {
            //done!
            log.info("reindex for " + id + " took " + Duration.ofMillis(TimeUtil.getCurrentTimeMillis() - reindexTimes.remove(id)));
            inMaintenanceMode = false;
            applicationEventPublisher.publishEvent(new MaintenanceModeEvent(MaintenanceModeEvent.Mode.END));
        }
    }

    @EventListener
    public synchronized void reindexEntity(IncrementReindexEvent event){
        SingleThreadCounter c = reindexCounts.get(event.getId());
        if(c !=null) {
            c.decrement();
            if (c.getAsLong() == 0L) {
                finishReindexEvent(event.getId());
            }
        }
    }
}
