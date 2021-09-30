package gsrs.scheduledTasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import gsrs.util.TaskListener;
import lombok.Data;

import java.util.function.Consumer;

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


    public Consumer<TaskListener> getRunner(){
        return this::run;
    }

    public abstract void run(TaskListener l);

    public abstract String getDescription();

    public SchedulerPlugin.ScheduledTask createTask(){
        return SchedulerPlugin.ScheduledTask.of(getRunner())
                .atCronTab(cron)
                .description(getDescription())
                .enable(this.isEnabled())
                ;
    }
}
