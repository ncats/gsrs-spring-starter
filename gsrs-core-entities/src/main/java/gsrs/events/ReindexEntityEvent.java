package gsrs.events;

import java.util.Optional;
import java.util.UUID;

import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class ReindexEntityEvent implements ReindexEvent {

	private UUID reindexId;
	private EntityUtils.Key entityKey;
	private boolean requiresDelete=false;
	private Optional<EntityWrapper<?>> optionalEntityWrapper = Optional.empty();


	public ReindexEntityEvent(UUID reindexId, EntityUtils.Key entityKey, Optional<EntityWrapper<?>> of, boolean b) {
		this.reindexId=reindexId;
		this.entityKey=entityKey;
		this.optionalEntityWrapper=of;
		this.requiresDelete=b;
	}
	public ReindexEntityEvent(UUID reindexId, EntityUtils.Key entityKey, Optional<EntityWrapper<?>> of) {
		this(reindexId,entityKey,of,false);
	}



	public Optional<EntityWrapper<?>> getOptionalFetchedEntityToReindex(){
		if(optionalEntityWrapper.isPresent())return optionalEntityWrapper;
		return entityKey.fetch();

	}

}
