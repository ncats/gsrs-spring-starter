package gsrs.indexer;

import ix.core.util.EntityUtils;
import org.springframework.context.ApplicationEvent;

public class IndexCreateEntityEvent extends ApplicationEvent {
    public IndexCreateEntityEvent(EntityUtils.EntityWrapper<?> source) {
        super(source);
    }

    @Override
    public EntityUtils.EntityWrapper<?> getSource() {
        return (EntityUtils.EntityWrapper<?>) super.getSource();
    }
}
