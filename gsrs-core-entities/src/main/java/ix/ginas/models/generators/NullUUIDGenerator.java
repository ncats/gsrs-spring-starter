package ix.ginas.models.generators;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.util.UUID;

public class NullUUIDGenerator implements IdentifierGenerator {

    public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        Object id = session.getEntityPersister(null, object).getIdentifier(object, session);
        return id != null ? id : UUID.randomUUID();
    }
}
