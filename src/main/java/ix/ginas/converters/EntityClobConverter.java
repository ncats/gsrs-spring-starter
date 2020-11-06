package ix.ginas.converters;


import org.hibernate.engine.jdbc.ClobProxy;

import javax.persistence.AttributeConverter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;

public abstract class EntityClobConverter<K> implements AttributeConverter<K, Clob> {


	public EntityClobConverter(){

	}

	@Override
	public Clob convertToDatabaseColumn(K k) {
		if(k==null){
			return null;
		}
		return ClobProxy.generateProxy(convertToString(k));
	}

	@Override
	public K convertToEntityAttribute(Clob clob) {

		try {
			String s = clob.getSubString(1, (int) clob.length());
			return convertFromString(s);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}


	protected abstract String convertToString(K value) ;
	protected abstract K convertFromString(String bytes);

    
}