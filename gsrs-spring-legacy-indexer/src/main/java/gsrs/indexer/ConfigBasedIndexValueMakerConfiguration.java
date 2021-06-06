package gsrs.indexer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.springUtils.AutowireHelper;
import ix.core.search.text.IndexValueMaker;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
