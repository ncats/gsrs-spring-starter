package gsrs.scheduler;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.scheduledTasks.SchedulerPlugin.ScheduledTask;
import gsrs.springUtils.AutowireHelper;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, Object> unknownParameters = new ConcurrentHashMap<>();

        @JsonAnySetter
        public void unknownSetter(String key, Object value){
            unknownParameters.put(key, value);
        }

    }

    private CachedSupplier<List<SchedulerPlugin.ScheduledTask>> tasks = CachedSupplier.of(()->{
        List<SchedulerPlugin.ScheduledTask> l = new ArrayList<>(list.size());
        ObjectMapper mapper = new ObjectMapper();
        for(ScheduledTaskConfig config : list){

            Map<String, Object> params;
            if(config.parameters ==null) {
                params = config.unknownParameters.isEmpty()? Collections.emptyMap(): config.unknownParameters;
            }else{
                params= config.parameters;
            }

            ScheduledTaskInitializer task = null;
            try {
            	System.out.println("Doing:" + config.scheduledTaskClass);
                task = (ScheduledTaskInitializer) mapper.convertValue(params, Class.forName(config.scheduledTaskClass));
            } catch (Exception e) {
            	JsonNode jsn= mapper.convertValue(params, JsonNode.class);
            	System.out.println(jsn.toPrettyString());
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
            task = AutowireHelper.getInstance().autowireAndProxy(task);
            ScheduledTask st = task.createTask();
            
            l.add(st);
            //TODO: need to fix this to happen in a more modular way, not with static methods
            // this is a hack to have things work for now.
            SchedulerPlugin.submit(st);


        }
        return l;

    });


    public List<SchedulerPlugin.ScheduledTask> getTasks(){
        return new ArrayList<>(tasks.get());
    }
    
    public void init() {
        tasks.get();
    }
}
