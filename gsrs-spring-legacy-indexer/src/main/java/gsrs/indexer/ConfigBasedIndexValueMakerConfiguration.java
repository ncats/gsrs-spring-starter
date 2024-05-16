package gsrs.indexer;

import com.fasterxml.jackson.annotation.JsonProperty;
import gsrs.util.ExtensionConfig;
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
    public static class IndexValueMakerConf implements ExtensionConfig {
        @JsonProperty("class")
        private Class entityClass;
        private Class indexer;
        private String parentKey;
        private Double order;
        private boolean disabled;

        private Map<String, Object> parameters;

    }

    @Bean
    @ConditionalOnMissingBean
    @Order
    public IndexValueMakerFactory indexValueMakerFactory(){
        String reportTag = "IndexValueMakerConf";
        Map<String, IndexValueMakerConf> map = list;
        if(map==null){
            return new ConfigBasedIndexValueMakerFactory(Collections.emptyList());
        }
        for (String k: map.keySet()) {
            map.get(k).setParentKey(k);
        }
        List<IndexValueMakerConf> configs = map.values().stream().collect(Collectors.toList());
        System.out.println("Indexer configurations found before filtering: " + configs.size());
        configs = configs.stream().filter(c->!c.isDisabled()).sorted(Comparator.comparing(c->c.getOrder(),nullsFirst(naturalOrder()))).collect(Collectors.toList());
        System.out.println(reportTag + " active after filtering: " + configs.size());
        System.out.printf("%s|%s|%s|%s|%s\n", reportTag, "class", "parentKey", "order", "isDisabled");
        for (IndexValueMakerConf config : configs) {
            System.out.printf("%s|%s|%s|%s|%s\n", reportTag, config.getIndexer(), config.getParentKey(), config.getOrder(), config.isDisabled());
        }
        return new ConfigBasedIndexValueMakerFactory(configs);
    }
}
