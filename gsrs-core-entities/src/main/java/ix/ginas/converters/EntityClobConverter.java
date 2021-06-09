package ix.ginas.converters;


import org.hibernate.engine.jdbc.ClobProxy;

import javax.persistence.AttributeConverter;
import java.sql.Clob;
import java.sql.SQLException;

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