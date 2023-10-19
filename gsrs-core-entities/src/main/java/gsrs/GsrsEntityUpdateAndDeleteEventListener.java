package gsrs;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.persister.entity.EntityPersister;

@Slf4j
public class GsrsEntityUpdateAndDeleteEventListener implements FlushEntityEventListener {

    public static final GsrsEntityUpdateAndDeleteEventListener INSTANCE = new GsrsEntityUpdateAndDeleteEventListener();

    @Override
    public void onFlushEntity(FlushEntityEvent event) throws HibernateException {
        final EntityEntry entry = event.getEntityEntry();
        final Object entity = event.getEntity();
        final boolean mightBeDirty = entry.requiresDirtyCheck(entity);

        if(mightBeDirty && entity instanceof ParentAware) {
            Object parent = ((ParentAware) entity).parentObject();
            //if (parent == null) {
            //    return;
            //}
            if(updated(event)) {
                log.debug("GsrsEntityUpdateAndDeleteEventListener onFlushEntity called for updated: " + entity.getClass().getName());
                incrementParentVersion(event, parent);
            } else if (deleted(event)) {
                log.debug("GsrsEntityUpdateAndDeleteEventListener onFlushEntity called for deleted: " + entity.getClass().getName());
                incrementParentVersion(event, parent);
            }
        }
    }

    private void incrementParentVersion(FlushEntityEvent event, Object parent) {
        if (parent == null) {
            return;
        }
        EntityEntry entityEntry = event.getSession().getPersistenceContext().getEntry(Hibernate.unproxy(parent));
        if(entityEntry.getStatus() != Status.DELETED) {
            event.getSession().lock(parent, LockMode.OPTIMISTIC_FORCE_INCREMENT);
        }
    }

    private boolean deleted(FlushEntityEvent event) {
        return event.getEntityEntry().getStatus() == Status.DELETED;
    }

    private boolean updated(FlushEntityEvent event) {
        final EntityEntry entry = event.getEntityEntry();
        final Object entity = event.getEntity();

        int[] dirtyProperties;
        EntityPersister persister = entry.getPersister();
        final Object[] values = event.getPropertyValues();
        SessionImplementor session = event.getSession();

        if ( event.hasDatabaseSnapshot() ) {
            dirtyProperties = persister.findModified(event.getDatabaseSnapshot(), values, entity, session);
        }
        else {
            dirtyProperties = persister.findDirty(values, entry.getLoadedState(), entity, session);
        }

        return dirtyProperties != null;
    }
}
