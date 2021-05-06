package gsrs.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.springUtils.AutowireHelper;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties("gsrs.scheduled-tasks")
public class GsrsSchedulerTaskPropertiesConfiguration {

    private List<ScheduledTaskConfig> list = new ArrayList<>();

    public List<ScheduledTaskConfig> getList() {
        return list;
    }

    public void setList(List<ScheduledTaskConfig> list) {
        this.list = list;
    }



    @Data
    public static class ScheduledTaskConfig{
        private String scheduledTaskClass;
        private Map<String, Object> parameters;

    }

    private CachedSupplier<List<ScheduledTaskInitializer>> tasks = CachedSupplier.of(()->{
        List<ScheduledTaskInitializer> l = new ArrayList<>(list.size());
        ObjectMapper mapper = new ObjectMapper();
        for(ScheduledTaskConfig config : list){

            Map<String, Object> params = config.parameters ==null? Collections.emptyMap() : config.parameters;

            ScheduledTaskInitializer task = null;
            try {
                task = (ScheduledTaskInitializer) mapper.convertValue(params, Class.forName(config.scheduledTaskClass));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
            AutowireHelper.getInstance().autowire(task);
            l.add(task);


        }
        return l;

    });


    public List<ScheduledTaskInitializer> getTasks(){
        return new ArrayList<>(tasks.get());
    }
}
