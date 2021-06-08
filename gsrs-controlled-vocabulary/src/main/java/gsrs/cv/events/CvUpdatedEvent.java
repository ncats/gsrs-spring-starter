package gsrs.cv.events;

import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import ix.ginas.models.v1.ControlledVocabulary;

public class CvUpdatedEvent extends AbstractEntityUpdatedEvent<ControlledVocabulary> {
    public CvUpdatedEvent(ControlledVocabulary source) {
        super(source);
    }
}
