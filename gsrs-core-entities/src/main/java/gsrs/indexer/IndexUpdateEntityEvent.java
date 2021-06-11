package gsrs.indexer;

import ix.core.util.EntityUtils;
import org.springframework.context.ApplicationEvent;

public class IndexUpdateEntityEvent extends ApplicationEvent {
    public IndexUpdateEntityEvent(EntityUtils.Key key) {
        super(key);
    }

    @Override
    public EntityUtils.Key getSource() {
        return (EntityUtils.Key) super.getSource();
    }
}
