package gsrs.events;

import org.springframework.context.ApplicationEvent;

public abstract class AbstractEntityUpdatedEvent<T> extends ApplicationEvent {
    public AbstractEntityUpdatedEvent(T source) {
        super(source);
    }

    @Override
    public T getSource() {
        return (T) super.getSource();
    }
}
