package gsrs.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;

@Profile("!test")
public class ScheduledTaskStartupRunner implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    GsrsSchedulerTaskPropertiesConfiguration config;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        config.init();
    }
}
