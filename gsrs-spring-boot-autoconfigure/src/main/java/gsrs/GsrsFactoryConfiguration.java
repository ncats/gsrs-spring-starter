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

@Component
@ConfigurationProperties("gsrs")
@Data
@Slf4j
public class GsrsFactoryConfiguration {

/*
gsrs.validators = {
"substances": {
    "IgnoreValidator": {
         "priority" : 10,
         "disabled": false,
         "validatorClass" = "ix.ginas.utils.validation.validators.IgnoreValidator",
         "newObjClass" = "ix.ginas.models.v1.Substance",
         "configClass" = "SubstanceValidatorConfig"
       }


*/

// maybe allow for a list if the map does not exist only if really needed.

    private Map<String, Map<String,Map<String, Object>>> validators;
    private Map<String, List<Map<String, Object>>> importAdapterFactories;

    private Map<String, List<Map<String, Object>>> matchableCalculators;

    private Map<String, Map<String, Object>> search;

    private List<EntityProcessorConfig> entityProcessors;

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
        if (entityProcessors == null) {
            //nothing set
            return Collections.emptyList();
        }
        return new ArrayList<>(entityProcessors);
    }


    public List<? extends ValidatorConfig> getValidatorConfigByContext(String context) {
        if (validators == null) {
            //nothing set
            return Collections.emptyList();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {

            Map<String,Map<String, Object>> map = validators.get(context);

            if (map == null || map.isEmpty()) {
                return Collections.emptyList();
            }
/*
By the time we are here all conf files have been processed
instantiate something that is a list, then iterate through everything
in the map, sort based on priority, and exclude anything that has disabled is true
map has a map.values.stream().sort(priority)  then filter(disabled=true).collect
*/

List<Object> list = map.values().stream().collect(Collectors.toList());

            List<? extends ValidatorConfig> configs = mapper.convertValue(list, new TypeReference<List<? extends ValidatorConfig>>() {
            });

//            List<ValidatorConfig> configs = new ArrayList<>();
//            for (Map<String,Object> n : list) {
//
//                Class<? extends ValidatorConfig> configClass = (Class<? extends ValidatorConfig>) n.get("configClass");
//                if(configClass ==null) {
//                    configs.add(mapper.convertValue(n, ValidatorConfig.class));
//                }else{
//                    configs.add(mapper.convertValue(n, configClass));
//                }
//            }
            return configs;
        } catch (Throwable t) {
            throw t;
        }
//        ValidatorConfigList list = (ValidatorConfigList) validators.get(context);
//        if(list ==null){
//            return Collections.emptyList();
//        }
//        return list.getConfigList();
    }

    /*
    retrieve a set of configuration items for the creation of AdapterFactory/ies based on
    context -- the name of a type of entity that the Adapters will create.
     */
    public List<? extends ImportAdapterFactoryConfig> getImportAdapterFactories(String context) {
        log.trace("starting in getImportAdapterFactories");
        if (importAdapterFactories == null) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> list = importAdapterFactories.get(context);
            log.trace("list (before):");
            list.forEach(i -> i.keySet().forEach(k -> log.trace("key: {}; value: {}", k, i.get(k))));

            if (list == null || list.isEmpty()) {
                log.warn("no import adapter factory configuration info found!");
                return Collections.emptyList();
            }
            List<? extends ImportAdapterFactoryConfig> configs = EntityUtils.convertClean(list, new TypeReference<List<? extends ImportAdapterFactoryConfig>>() {
            });
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
        if(matchableCalculators==null){
            return Collections.emptyList();
        }
        List<Map<String, Object>> list = matchableCalculators.get(context);
        if (list == null || list.isEmpty()) {
            log.warn("no matchable calculation configuration info found!");
            return Collections.emptyList();
        }
        List<? extends MatchableCalculationConfig> configs = EntityUtils.convertClean(list, new TypeReference<List<? extends MatchableCalculationConfig>>() {
        });
        //log.trace("list (after):");
        //configs.forEach(c-> log.trace("name: {}; desc: {}; ext: {}", c.getAdapterName(), c.getDescription(), c.getSupportedFileExtensions()));
        return configs;
    }
}

