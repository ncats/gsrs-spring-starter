package gsrs.indexer;

import ix.core.util.EntityUtils;
import org.springframework.context.ApplicationEvent;

public class IndexCreateEntityEvent extends ApplicationEvent {
    private boolean deleteFirst;

    public IndexCreateEntityEvent(EntityUtils.Key key) {
        super(key);
    }

    @Override
    public EntityUtils.Key  getSource() {
        return (EntityUtils.Key ) super.getSource();
    }

    public boolean shouldDeleteFirst(){
        return isDeleteFirst();
    }
    public boolean isDeleteFirst() {
        return deleteFirst;
    }

    public void setDeleteFirst(boolean deleteFirst) {
        this.deleteFirst = deleteFirst;
    }
}
