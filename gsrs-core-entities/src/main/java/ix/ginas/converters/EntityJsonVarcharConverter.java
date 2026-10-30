package ix.ginas.converters;

import ix.core.controllers.EntityFactory;
import ix.core.controllers.EntityFactory.EntityMapper;
import tools.jackson.core.JacksonException;

public abstract class EntityJsonVarcharConverter<K> extends EntityVarcharConverter<K> {
	public EntityMapper em =  EntityFactory.EntityMapper.FULL_ENTITY_MAPPER();
	private Class<K> cls;
	
	public EntityJsonVarcharConverter(Class<K> cls) {
		this.cls=cls;
	}

	@Override
	protected String convertToString(K value)  {
		return em.toJson(value);
	}

	@Override
	protected K convertFromString(String bytes){
		if(bytes==null)return null;
		try {
			return em.readValue(bytes, cls);
		} catch (JacksonException e) {
			throw new IllegalStateException(e);
		}
	}
}
