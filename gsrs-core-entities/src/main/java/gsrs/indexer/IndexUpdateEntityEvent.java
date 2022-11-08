package gsrs.indexer;

import java.util.Optional;

import org.springframework.context.ApplicationEvent;

import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;

public class IndexUpdateEntityEvent extends ApplicationEvent {

	private Optional<EntityWrapper<?>> optionalEntityWrapper = Optional.empty();

    public IndexUpdateEntityEvent(EntityUtils.Key key) {
        this(key, Optional.empty());
    }
    
    public IndexUpdateEntityEvent(EntityUtils.Key key, Optional<EntityWrapper<?>> optionalEntityWrapper) {
        super(key);
        this.optionalEntityWrapper=optionalEntityWrapper;
    }

    @Override
    public EntityUtils.Key getSource() {
        return (EntityUtils.Key) super.getSource();
    }
    

	public Optional<EntityWrapper<?>> getOptionalFetchedEntity(){
		if(optionalEntityWrapper.isPresent())return optionalEntityWrapper;
		return getSource().fetch();

	}
    
}
