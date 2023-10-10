package gsrs;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.annotation.Autowired;


public class GsrsEntityUpdateAndDeleteEventListener implements FlushEntityEventListener {

    public static final GsrsEntityUpdateAndDeleteEventListener INSTANCE = new GsrsEntityUpdateAndDeleteEventListener();
    private boolean enabled = true;

    @Autowired
    public void setDefaultDataSourceConfig(DefaultDataSourceConfig defaultDataSourceConfig) {
        if (defaultDataSourceConfig.additionalJpaProperties("spring").get("hibernate.entity_dirtiness_strategy") != null) {
            enabled = false;
        }
    }

    @Override
    public void onFlushEntity(FlushEntityEvent event) throws HibernateException {
        if (!enabled) return;
        final EntityEntry entry = event.getEntityEntry();
        final Object entity = event.getEntity();
        final boolean mightBeDirty = entry.requiresDirtyCheck(entity);

        if(mightBeDirty && entity instanceof ParentAware) {
            Object parent = ((ParentAware) entity).parentObject();
            if (parent == null) {
                return;
            } else if(updated(event)) {
                incrementParentVersion(event, parent);
            } else if (deleted(event)) {
                incrementParentVersion(event, parent);
            }
        }
    }

    private void incrementParentVersion(FlushEntityEvent event, Object parent) {
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
