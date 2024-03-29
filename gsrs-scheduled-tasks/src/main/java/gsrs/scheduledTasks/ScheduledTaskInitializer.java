package gsrs.scheduledTasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.function.BiConsumer;

/**
 * Abstract Class to create ScheduledTasks.
 *
 */
@Data
public abstract class ScheduledTaskInitializer {
    @JsonProperty("autorun")
    private boolean enabled = false;
    
    @JsonProperty("autorun")
    public void setEnabled(boolean b) {
        this.enabled=b;
    }

    private String cron=CronExpressionBuilder.builder()
            .everyDay()
            .atHourAndMinute(2, 04)
            .build();


    public BiConsumer<gsrs.scheduledTasks.SchedulerPlugin.JobStats, SchedulerPlugin.TaskListener> getRunner(){
        return this::run;
    }

    public abstract void run(gsrs.scheduledTasks.SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l);

    public abstract String getDescription();

    public SchedulerPlugin.ScheduledTask createTask(){
        return SchedulerPlugin.ScheduledTask.of(getRunner())
                .atCronTab(cron)
                .description(getDescription())
                .enable(this.isEnabled())
                ;
    }
}
