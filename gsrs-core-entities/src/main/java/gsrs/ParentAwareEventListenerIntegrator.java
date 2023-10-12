package gsrs;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;


public class ParentAwareEventListenerIntegrator implements Integrator {

    public static final ParentAwareEventListenerIntegrator INSTANCE =
        new ParentAwareEventListenerIntegrator();

    @Override
    public void integrate(
            Metadata metadata,
            SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {

        final EventListenerRegistry eventListenerRegistry =
                serviceRegistry.getService(
                    EventListenerRegistry.class
        );

        eventListenerRegistry.appendListeners(
            EventType.PERSIST,
            GsrsEntityInsertEventListener.INSTANCE
        );
        eventListenerRegistry.appendListeners(
            EventType.FLUSH_ENTITY,
            GsrsEntityUpdateAndDeleteEventListener.INSTANCE
        );
    }

    @Override
    public void disintegrate(
            SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {
    }
}
