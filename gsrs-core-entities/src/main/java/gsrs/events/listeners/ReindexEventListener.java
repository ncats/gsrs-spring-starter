package gsrs.events.listeners;

import gov.nih.ncats.common.util.SingleThreadCounter;
import gsrs.events.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listens for reindex events {@link BeginReindexEvent} and counts
 * of each {@link ReindexEntityEvent} to figure out when {@link EndReindexEvent} and
 * {@link MaintenanceModeEvent} should fire fired (and then publishes those events).
 */
@Service
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
        reindexCounts.put(event.getId(), new SingleThreadCounter(event.getNumberOfExpectedRecord()));
        reindexTimes.put(event.getId(), System.currentTimeMillis());
        if(!inMaintenanceMode){
            inMaintenanceMode=true;
            //was not in maintenanceMode mode before and now it is
            applicationEventPublisher.publishEvent(new MaintenanceModeEvent(MaintenanceModeEvent.Mode.BEGIN));
        }
    }

    @EventListener
    public synchronized void reindexEntity(IncrementReindexEvent event){
        SingleThreadCounter c = reindexCounts.get(event.getId());
        if(c !=null) {
            c.decrement();

            if (c.getAsLong() == 0L) {
                reindexCounts.remove(event.getId());
                applicationEventPublisher.publishEvent(new EndReindexEvent(event.getId()));
                if (reindexCounts.isEmpty()) {
                    //done!
                    System.out.println("reindex for " + event.getId() + " took " + Duration.ofMillis(System.currentTimeMillis() - reindexTimes.remove(event.getId())));
                    inMaintenanceMode = false;
                    applicationEventPublisher.publishEvent(new MaintenanceModeEvent(MaintenanceModeEvent.Mode.END));
                }
            }
        }
    }
}
