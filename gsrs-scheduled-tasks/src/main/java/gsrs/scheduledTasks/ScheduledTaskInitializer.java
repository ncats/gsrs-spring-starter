package gsrs.scheduledTasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.function.Consumer;

@Data
public abstract class ScheduledTaskInitializer {
    @JsonProperty("autorun")
    private boolean enabled;

    private String cron=CronExpressionBuilder.builder()
            .everyDay()
            .atHourAndMinute(2, 04)
            .build();


    public Consumer<SchedulerPlugin.TaskListener> getRunner(){
        return this::run;
    }

    public abstract void run(SchedulerPlugin.TaskListener l);

    public abstract String getDescription();

    public SchedulerPlugin.ScheduledTask createTask(){
        return SchedulerPlugin.ScheduledTask.of(getRunner())
                .atCronTab(cron)
                .description(getDescription());
    }
}
