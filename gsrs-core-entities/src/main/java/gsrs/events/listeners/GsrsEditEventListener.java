package gsrs.events.listeners;

import gsrs.events.CreateEditEvent;
import gsrs.services.EditEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Spring Event Listener that will receive {@link CreateEditEvent}s
 * when the transaction commits and delegates to the {@link EditEventService}.
 */
@Component
public class GsrsEditEventListener {
    @Autowired
    private EditEventService editEventService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createEditEvent(CreateEditEvent event){
        editEventService.createNewEditFromEvent(event);
    }
}
