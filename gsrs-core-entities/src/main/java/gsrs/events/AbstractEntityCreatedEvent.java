package gsrs.events;

import org.springframework.context.ApplicationEvent;

public abstract class AbstractEntityCreatedEvent<T> extends ApplicationEvent {
    public AbstractEntityCreatedEvent(T source) {
        super(source);
    }

    @Override
    public T getSource() {
        return (T) super.getSource();
    }
}
