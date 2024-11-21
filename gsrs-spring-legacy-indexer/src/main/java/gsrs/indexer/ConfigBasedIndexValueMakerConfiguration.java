package gsrs.indexer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import ix.core.search.text.IndexValueMaker;
import lombok.Data;

@Configuration
@ConfigurationProperties("gsrs.indexers")
@Data
public class ConfigBasedIndexValueMakerConfiguration {

    private boolean includeDefaultIndexers=true;

    private List<IndexValueMakerConf> list;

    @Data
    public static class IndexValueMakerConf{
        @JsonProperty("class")
        private Class entityClass;

        private Class indexer;

        private Map<String, Object> parameters;


        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, Object> unknownParameters = new ConcurrentHashMap<>();

        @JsonAnySetter
        public void unknownSetter(String key, Object value){
            unknownParameters.put(key, value);
        }
        
        public IndexValueMaker newIndexValueMaker(ObjectMapper mapper, ClassLoader classLoader) throws ClassNotFoundException {

            if(parameters !=null && !parameters.isEmpty()){
                return (IndexValueMaker) mapper.convertValue(parameters, indexer);
            }
            if(unknownParameters !=null && !unknownParameters.isEmpty()){
                return (IndexValueMaker) mapper.convertValue(unknownParameters, indexer);

            }
            return (IndexValueMaker) mapper.convertValue(Collections.emptyMap(), indexer);
        }



    }


    @Bean
    @ConditionalOnMissingBean
    @Order
    public IndexValueMakerFactory indexValueMakerFactory(){
        if(list==null){
            return new ConfigBasedIndexValueMakerFactory(Collections.emptyList());
        }



        return new ConfigBasedIndexValueMakerFactory(list);
    }
}
