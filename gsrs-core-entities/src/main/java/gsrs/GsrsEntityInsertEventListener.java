package gsrs;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class GsrsEntityInsertEventListener implements PersistEventListener {

    public static final GsrsEntityInsertEventListener INSTANCE = new GsrsEntityInsertEventListener();
    private boolean enabled = true;

    @Autowired
    public void setDefaultDataSourceConfig(DefaultDataSourceConfig defaultDataSourceConfig) {
        log.info("Check the hibernate.entity_dirtiness_strategy value");
        if (defaultDataSourceConfig.additionalJpaProperties("spring").get("hibernate.entity_dirtiness_strategy") != null) {
            enabled = false;
        }
    }

    @Override
    public void onPersist(PersistEvent event) throws HibernateException {
        if (!enabled) return;
        log.info("GsrsEntityInsertEventListener - enabled");
        final Object entity = event.getObject();

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
