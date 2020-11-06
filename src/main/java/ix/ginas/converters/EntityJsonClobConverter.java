package ix.ginas.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import ix.core.controllers.EntityFactory;
import ix.core.controllers.EntityFactory.EntityMapper;

import java.io.IOException;

public abstract class EntityJsonClobConverter<K> extends EntityClobConverter<K> {
	public EntityMapper em =  EntityFactory.EntityMapper.FULL_ENTITY_MAPPER();
	private Class<K> cls;
	
	public EntityJsonClobConverter(Class<K> cls) {
		this.cls=cls;
	}

	@Override
	public String convertToString(K value){
		return em.toJson(value);
	}

	@Override
	public K convertFromString(String bytes){
		if(bytes==null){
			return null;
		}
		try {
			return em.readValue(bytes, cls);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}