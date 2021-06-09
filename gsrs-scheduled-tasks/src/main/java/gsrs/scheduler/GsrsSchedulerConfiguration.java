package gsrs.scheduler;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GsrsSchedulerConfiguration {

//    @Bean
//    @ConditionalOnMissingBean
//    public Scheduler getScheduler() throws SchedulerException {
//        return StdSchedulerFactory.getDefaultScheduler();
//    }
}
