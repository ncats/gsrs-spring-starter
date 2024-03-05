package gsrs.indexer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

@Configuration
@ConfigurationProperties("gsrs.indexers")
@Data
public class ConfigBasedIndexValueMakerConfiguration {

    private boolean includeDefaultIndexers=true;

    private Map<String, IndexValueMakerConf> list;

    @Data
    public static class IndexValueMakerConf{
        @JsonProperty("class")
        private Class entityClass;
        private Class indexer;
        private String key;
        private Double order;
        private boolean disabled;

        private Map<String, Object> parameters;

    }

    @Bean
    @ConditionalOnMissingBean
    @Order
    public IndexValueMakerFactory indexValueMakerFactory(){

        Map<String, IndexValueMakerConf> map =  list;

        if(map==null){
            return new ConfigBasedIndexValueMakerFactory(Collections.emptyList());
        }

        // Copy the key into the Object for quality control and maybe as a way to access by key from the list
        for (String k: map.keySet()) {
            map.get(k).setKey(k);
        }

        // By the time we are here all conf files have been processed

        List<IndexValueMakerConf> configs = map.values().stream().collect(Collectors.toList());

        System.out.println("Indexer configurations found before filtering: " + configs.size());

        configs = configs.stream().filter(p->!p.isDisabled()).sorted(Comparator.comparing(i->i.getOrder(),nullsFirst(naturalOrder()))).collect(Collectors.toList());

        System.out.println("Indexer configurations active after filtering: " + configs.size());

        System.out.println(String.format("%s|%s|%s|%s", "Indexer", "class", "key", "order", "isDisabled"));
        for (IndexValueMakerConf config : configs) {
            System.out.println(String.format("%s|%s|%s|%s", "Indexer", config.getIndexer(), config.getKey(), config.getOrder(), config.isDisabled()));
        }
        return new ConfigBasedIndexValueMakerFactory(configs);
    }
}
