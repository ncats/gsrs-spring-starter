package gsrs.indexer;

import ix.core.util.EntityUtils;
import org.springframework.context.ApplicationEvent;

public class IndexCreateEntityEvent extends ApplicationEvent {
    private boolean deleteFirst;

    public IndexCreateEntityEvent(EntityUtils.EntityWrapper<?> source) {
        super(source);
    }

    @Override
    public EntityUtils.EntityWrapper<?> getSource() {
        return (EntityUtils.EntityWrapper<?>) super.getSource();
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
