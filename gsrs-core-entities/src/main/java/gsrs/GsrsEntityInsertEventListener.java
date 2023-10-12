package gsrs;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;

@Slf4j
public class GsrsEntityInsertEventListener implements PersistEventListener {

    public static final GsrsEntityInsertEventListener INSTANCE = new GsrsEntityInsertEventListener();

    @Override
    public void onPersist(PersistEvent event) throws HibernateException {
        final Object entity = event.getObject();
        log.debug("GsrsEntityInsertEventListener onPersist called for: " + entity.getClass().getName());

        if(entity instanceof ParentAware){
            Object parent = ((ParentAware) entity).parentObject();
            if (parent != null) {
                event.getSession().lock(parent, LockMode.OPTIMISTIC_FORCE_INCREMENT);
            }
        }
    }

    @Override
    public void onPersist(PersistEvent event, Map createdAlready) throws HibernateException {
        onPersist(event);
    }
}
