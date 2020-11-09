package ix.ginas.converters;



import org.hibernate.engine.jdbc.BlobProxy;

import javax.persistence.AttributeConverter;
import java.sql.Blob;
import java.sql.SQLException;

public abstract class EntityBlobConverter<K> implements AttributeConverter<K, Blob> {
	
	
	public EntityBlobConverter(){

	}

	@Override
	public Blob convertToDatabaseColumn(K k) {
		if(k==null){
			return null;
		}
		return BlobProxy.generateProxy(convertToBytes(k));
	}

	@Override
	public K convertToEntityAttribute(Blob blob) {

		try {
			byte[] bytes = blob.getBytes(1, (int) blob.length());
			return convertFromBytes(bytes);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	protected abstract byte[] convertToBytes(K value);
	protected abstract K convertFromBytes(byte[] bytes);

}