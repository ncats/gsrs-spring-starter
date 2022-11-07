package gsrs.indexer;

import java.util.Optional;

import org.springframework.context.ApplicationEvent;

import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;

public class IndexCreateEntityEvent extends ApplicationEvent {
    private boolean deleteFirst;

	private Optional<EntityWrapper<?>> optionalEntityWrapper = Optional.empty();
	
    public IndexCreateEntityEvent(EntityUtils.Key key, Optional<EntityWrapper<?>> optionalEntityWrapper) {
        super(key);
        if(optionalEntityWrapper==null)optionalEntityWrapper=Optional.empty();
        this.optionalEntityWrapper=optionalEntityWrapper;
    }
    public IndexCreateEntityEvent(EntityUtils.Key key) {
        this(key,Optional.empty());
    }
    

	public Optional<EntityWrapper<?>> getOptionalFetchedEntityToIndex(){
		if(optionalEntityWrapper.isPresent())return optionalEntityWrapper;
		return this.getSource().fetch();

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
