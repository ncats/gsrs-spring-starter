package gsrs.scheduler;

import gsrs.scheduledTasks.SchedulerPlugin;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties("gsrs.scheduled-tasks")
@Data
public class GsrsSchedulerConfiguration {

    private List<SchedulerPlugin.ScheduledTask> list = new ArrayList<>();


}
