package ix.ginas.converters;



import javax.persistence.AttributeConverter;


public abstract class EntityVarcharConverter<K> implements AttributeConverter<K, String> {
	//hibernate should convert String to varchar

	public EntityVarcharConverter(){

	}

	@Override
	public String convertToDatabaseColumn(K k) {
		if(k==null){
			return null;
		}
		return convertToString(k);
	}

	@Override
	public K convertToEntityAttribute(String s) {
		return convertFromString(s);

	}


	protected abstract String convertToString(K value) ;
	protected abstract K convertFromString(String bytes);
    
}