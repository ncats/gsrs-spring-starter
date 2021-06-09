package gsrs.indexer;

import ix.core.util.EntityUtils;
import org.springframework.context.ApplicationEvent;

public class IndexRemoveEntityEvent extends ApplicationEvent {
    public IndexRemoveEntityEvent(EntityUtils.EntityWrapper source) {
        super(source);
    }

    @Override
    public EntityUtils.EntityWrapper getSource() {
        return (EntityUtils.EntityWrapper) super.getSource();
    }
}
