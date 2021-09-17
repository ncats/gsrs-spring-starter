package ix.core.initializers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
import lombok.Data;

@Configuration
@ConfigurationProperties("gsrs.initializers")
public class GsrsInitializerPropertiesConfiguration {

    private List<InitializerConfig> list = new ArrayList<>();

    public List<InitializerConfig> getList() {
        return list;
    }

    public void setList(List<InitializerConfig> list) {
        this.list = list;
    }
    
    @Data
    public static class InitializerConfig{
        private String initializerClass;
        private Map<String, Object> parameters;
    }

    private CachedSupplier<List<Initializer>> tasks = CachedSupplier.of(()->{
        List<Initializer> l = new ArrayList<>(list.size());
        ObjectMapper mapper = new ObjectMapper();
        for(InitializerConfig config : list){

            Map<String, Object> params = config.parameters ==null? Collections.emptyMap() : config.parameters;

            Initializer task = null;
            try {
                task = (Initializer) mapper.convertValue(params, Class.forName(config.initializerClass));
                
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
            task = AutowireHelper.getInstance().autowireAndProxy(task);
            
            l.add(task);

        }
        return l;

    });


    public List<Initializer> getInitializers(){
        return new ArrayList<>(tasks.get());
    }
}
