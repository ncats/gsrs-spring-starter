package gsrs.events.listeners;

import gsrs.events.CreateEditEvent;
import gsrs.services.EditEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

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
