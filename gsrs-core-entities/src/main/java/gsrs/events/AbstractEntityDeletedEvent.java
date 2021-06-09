package gsrs.events;

import org.springframework.context.ApplicationEvent;

public abstract class AbstractEntityDeletedEvent<T> extends ApplicationEvent {
    public AbstractEntityDeletedEvent(T source) {
        super(source);
    }

    @Override
    public T getSource() {
        return (T) super.getSource();
    }
}
