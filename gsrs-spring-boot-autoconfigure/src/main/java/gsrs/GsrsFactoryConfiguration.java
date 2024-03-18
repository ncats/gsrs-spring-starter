package gsrs;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.entityProcessor.EntityProcessorConfig;
import gsrs.imports.ImportAdapterFactoryConfig;
import gsrs.imports.MatchableCalculationConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.util.EntityUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

@Component
@ConfigurationProperties("gsrs")
@Data
@Slf4j
public class GsrsFactoryConfiguration {

    private Map<String, Map<String, Map<String, Object>>> validators;

    private Map<String, Map<String, Map<String, Object>>> importAdapterFactories;

    private Map<String, Map<String, Map<String, Object>>> matchableCalculators;

    private Map<String, Map<String, Object>> search;

    private Map<String, EntityProcessorConfig> entityProcessors;

    private boolean createUnknownUsers = false;

    private Map<String, String> defaultStagingAreaServiceClass;

    private Map<String, String> defaultStagingAreaEntityService;

    private Map<String, List<String>> availableProcessActions;

    private Map<String, String> uuidCodeSystem;

    private Map<String, String> approvalIdCodeSystem;

    private Map<String, Boolean> sortExportOutput;

    public Optional<Map<String, Object>> getSearchSettingsFor(String context) {
        if (search == null) return Optional.empty();
        return Optional.ofNullable(search.get(context));
    }

    public List<EntityProcessorConfig> getEntityProcessors() {
        String reportTag = "EntityProcessorConfig";
        if (entityProcessors == null) {
            //nothing set
            return Collections.emptyList();
        }
        Map<String,EntityProcessorConfig> map =  entityProcessors;
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        for (String k: map.keySet()) {
            map.get(k).setParentKey(k);
        }
        List<EntityProcessorConfig> configs = map.values().stream().collect(Collectors.toList());
        System.out.println(reportTag + " found before filtering: " + configs.size());
        configs = configs.stream().filter(c->!c.isDisabled()).sorted(Comparator.comparing(c->c.getOrder(),nullsFirst(naturalOrder()))).collect(Collectors.toList());
        System.out.println(reportTag + " active after filtering: " + configs.size());
        System.out.println(String.format("%s|%s|%s|%s", reportTag, "class", "parentKey", "order", "isDisabled"));
        for (EntityProcessorConfig config : configs) {
            System.out.println(String.format("%s|%s|%s|%s", reportTag, config.getProcessor(), config.getParentKey(), config.getOrder(), config.isDisabled()));
        }
        return configs;
    }

    public List<? extends ValidatorConfig> getValidatorConfigByContext(String context) {
        String reportTag = "ValidatorConfig";
        if (validators == null) {
            return Collections.emptyList();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String,Map<String, Object>> map = (Map<String, Map<String, Object>>) validators.get(context);
            if (map == null || map.isEmpty()) {
                return Collections.emptyList();
            }
            for (String k: map.keySet()) {
                map.get(k).put("parentKey", k);
            }
            List<Object> list = map.values().stream().collect(Collectors.toList());
            List<? extends ValidatorConfig> configs = mapper.convertValue(list, new TypeReference<List<? extends ValidatorConfig>>() { });
            System.out.println( reportTag + "found before filtering: " + configs.size());
            configs = configs.stream().filter(c->!c.isDisabled()).sorted(Comparator.comparing(c->c.getOrder(),nullsFirst(naturalOrder()))).collect(Collectors.toList());
            System.out.println(reportTag + " active after filtering: " + configs.size());
            System.out.println(String.format("%s|%s|%s|%s", reportTag, "class", "parentKey", "order", "isDisabled"));
            for (ValidatorConfig config : configs) {
                System.out.println(String.format("%s|%s|%s|%s", reportTag, config.getValidatorClass(), config.getParentKey(), config.getOrder(), config.isDisabled()));
            }
            return configs;
        } catch (Throwable t) {
            throw t;
        }
    }

    /*
    retrieve a set of configuration items for the creation of AdapterFactory/ies based on
    context -- the name of a type of entity that the Adapters will create.
     */
    public List<? extends ImportAdapterFactoryConfig> getImportAdapterFactories(String context) {
        String reportTag = "ImportAdapterFactoryConfig";
        log.trace("starting in getImportAdapterFactories");
        if (importAdapterFactories == null) {
            return Collections.emptyList();
        }
        try {
            Map<String, Map<String, Object>> map = importAdapterFactories.get(context);
            log.trace("map (before):");

            // fix this later
            // list.forEach(i -> i.keySet().forEach(k -> log.trace("key: {}; value: {}", k, i.get(k))));

//            map.keySet().forEach(k1->{
//                map.get(k1).keySet().forEach(k2-> log.trace("key: {}; value: {}", k2, map.get(k1).get(k2)));
//            });

            if (map == null || map.isEmpty()) {
                log.warn("no import adapter factory configuration info found!");
                return Collections.emptyList();
            }
            for (String k: map.keySet()) {
                map.get(k).put("parentKey", k);
            }
            List<Object> list = map.values().stream().collect(Collectors.toList());
            List<? extends ImportAdapterFactoryConfig> configs = EntityUtils.convertClean(list, new TypeReference<List<? extends ImportAdapterFactoryConfig>>() { });
            System.out.println(reportTag + "  found before filtering: " + configs.size());
            configs = configs.stream().filter(c->!c.isDisabled()).sorted(Comparator.comparing(c->c.getOrder(),nullsFirst(naturalOrder()))).collect(Collectors.toList());
            System.out.println(reportTag + " active after filtering: " + configs.size());
            System.out.println(String.format("%s|%s|%s|%s", reportTag, "class", "parentKey", "order", "isDisabled"));
            for (ImportAdapterFactoryConfig config : configs) {
                System.out.println(String.format("%s|%s|%s|%s", reportTag, config.getImportAdapterFactoryClass(), config.getParentKey(), config.getOrder(), config.isDisabled()));
            }

            //log.trace("list (after):");
            //configs.forEach(c-> log.trace("name: {}; desc: {}; ext: {}", c.getAdapterName(), c.getDescription(), c.getSupportedFileExtensions()));
            return configs;
        } catch (Exception t) {
            log.error("Error fetching import factory config");
            throw t;
        }
    }

    public List<? extends MatchableCalculationConfig> getMatchableCalculationConfig(String context) {
        log.trace("in ");
        String reportTag = "MatchableCalculationConfig";
        if(matchableCalculators==null){
            return Collections.emptyList();
        }
        try {
            Map<String, Map<String, Object>> map = matchableCalculators.get(context);
            if (map == null || map.isEmpty()) {
                log.warn("no matchable calculation configuration info found!");
                return Collections.emptyList();
            }
            // Copy the key into the Object for quality control and maybe as a way to access by key from the list
            for (String k: map.keySet()) {
                map.get(k).put("parentKey", k);
            }
            List<Object> list = map.values().stream().collect(Collectors.toList());
            List<? extends MatchableCalculationConfig> configs = EntityUtils.convertClean(list, new TypeReference<List<? extends MatchableCalculationConfig>>() { });
            System.out.println(reportTag + " found before filtering: " + configs.size());
            configs = configs.stream().filter(c->!c.isDisabled()).sorted(Comparator.comparing(c->c.getOrder(),nullsFirst(naturalOrder()))).collect(Collectors.toList());
            System.out.println(reportTag + " active after filtering: " + configs.size());
            System.out.println(String.format("%s|%s|%s|%s", "MatchableCalculator", "class", "parentKey", "order", "isDisabled"));
            for (MatchableCalculationConfig config : configs) {
                System.out.println(String.format("%s|%s|%s|%s", reportTag, config.getMatchableCalculationClass(), config.getParentKey(), config.getOrder(), config.isDisabled()));
            }
            return configs;
        } catch (Throwable t) {
            throw t;
        }
    }

}

