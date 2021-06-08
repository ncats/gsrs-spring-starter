package gsrs.cv.events;

import gsrs.events.AbstractEntityCreatedEvent;
import ix.ginas.models.v1.ControlledVocabulary;

public class CvCreatedEvent extends AbstractEntityCreatedEvent<ControlledVocabulary> {
    public CvCreatedEvent(ControlledVocabulary source) {
        super(source);
    }
}
