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
import gsrs.util.ExtensionConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

@Configuration
@ConfigurationProperties("gsrs.scheduled-tasks")
public class GsrsSchedulerTaskPropertiesConfiguration {

    private Map<String, ScheduledTaskConfig> list = new HashMap<String, ScheduledTaskConfig>();

    public Map<String, ScheduledTaskConfig> getList() {
        return list;
    }

    public void setList(Map<String, ScheduledTaskConfig> list) {
        this.list = list;
    }

    // public void setList(List<ScheduledTaskConfig> list) { this.list = list; }

    @Data
    public static class ScheduledTaskConfig implements ExtensionConfig {
        private String scheduledTaskClass;
        private String parentKey;
        private Double order;
        private boolean disabled = false;
        private Map<String, Object> parameters;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, Object> unknownParameters = new ConcurrentHashMap<>();

        @JsonAnySetter
        public void unknownSetter(String key, Object value){
            unknownParameters.put(key, value);
        }

    }

    // Use this to hold configs list before they are made into tasks.
    // That way we can share this list via an api endpoint
    // Make sure this values is always quashed each time
    // The cached supplier is refreshed
    private List<ScheduledTaskConfig> configs = null;

    private CachedSupplier<List<SchedulerPlugin.ScheduledTask>> tasks = CachedSupplier.of(()->{
        String reportTag = "ScheduledTaskConfig";
        List<SchedulerPlugin.ScheduledTask> l = new ArrayList<>(list.size());
        ObjectMapper mapper = new ObjectMapper();
        for (String k: list.keySet()) {
            ScheduledTaskConfig config =  list.get(k);
            config.setParentKey(k);
        }
        // This variable has class scope, is there any problem with that?
        configs = list.values().stream().collect(Collectors.toList());
        System.out.println(reportTag + " found before filtering: " + configs.size());
        configs = configs.stream().filter(p->!p.isDisabled()).sorted(Comparator.comparing(i->i.getOrder(),nullsFirst(naturalOrder()))).collect(Collectors.toList());
        System.out.println(reportTag + " active after filtering: " + configs.size());
        System.out.printf("%s|%s|%s|%s|%s\n", "reportTag", "class", "parentKey", "order", "isDisabled");
        for (ScheduledTaskConfig config : configs) {
            System.out.printf("%s|%s|%s|%s|%s\n", reportTag, config.getScheduledTaskClass(), config.getParentKey(), config.getOrder(), config.isDisabled());
        }

        for(ScheduledTaskConfig config : configs){

            Map<String, Object> params;
            if(config.parameters ==null) {
                params = config.unknownParameters.isEmpty()? Collections.emptyMap(): config.unknownParameters;
            }else{
                params= config.parameters;
            }

            ScheduledTaskInitializer task = null;
            try {
            	System.out.println("Doing: " + config.scheduledTaskClass);
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

    public List<ScheduledTaskConfig> getConfigs()  {
        return this.configs;
    }

    public List<SchedulerPlugin.ScheduledTask> getTasks(){
        return new ArrayList<>(tasks.get());
    }
    
    public void init() {
        tasks.get();
    }
}
