package gsrs;

import akka.event.Logging;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.entityProcessor.EntityProcessorConfig;
import gsrs.imports.DefaultImportAdapterFactoryConfig;
import gsrs.imports.ImportAdapterFactoryConfig;
import gsrs.validator.ValidatorConfig;
import gsrs.validator.ValidatorConfigList;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties("gsrs")
@Data
@Slf4j
public class GsrsFactoryConfiguration {

    private Map<String, List<Map<String,Object>>> validators;
    private Map<String, List<Map<String,Object>>> importAdapterFactories;
    private List<EntityProcessorConfig> entityProcessors;

    private boolean createUnknownUsers= false;

    @Value("${gsrs.importAdapterFactories.substances}")
    private String rawImportConfigString;

    public List<EntityProcessorConfig> getEntityProcessors(){
        if(entityProcessors ==null){
            //nothing set
            return Collections.emptyList();
        }
        return new ArrayList<>(entityProcessors);
    }



    public List<? extends ValidatorConfig> getValidatorConfigByContext(String context){
        if(validators ==null){
            //nothing set
            return Collections.emptyList();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Map<String,Object>> list = validators.get(context);

            if(list==null || list.isEmpty()){
                return Collections.emptyList();
            }
            List<? extends ValidatorConfig> configs = mapper.convertValue(list, new TypeReference<List<? extends ValidatorConfig>>() {});

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
        }catch(Throwable t){
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
        log.warn("starting in getImportAdapterFactories. context: " +context);
        log.warn("rawImportConfigString: " + rawImportConfigString);
        String decodedConfig="";
        try {
            decodedConfig = URLDecoder.decode(rawImportConfigString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if(importAdapterFactories ==null) {

            return Collections.emptyList();
        }
        ObjectMapper mapper = new ObjectMapper();

        try {
            /*List<Map<String, Object>> valueList = importAdapterFactories.get(context);
            if(valueList==null || valueList.isEmpty()){
                log.warn("no import adapter factory configuration info found!");
                return Collections.emptyList();
            }
            log.warn("list:");
            valueList.forEach(i->i.keySet().forEach(k-> {
                log.warn(String.format("key: %s; value: %s; value type: %s",
                        k, i.get(k), i.get(k).getClass().getName()));
                *//*if(i.get(k) instanceof LinkedHashMap) {
                    Map map = (Map)i.get(k);
                    List singleSet = (List) map.values().stream().collect(Collectors.toList());
                    log.warn("going to replace value for key " + k);
                    i.put(k, singleSet);
                }*//*
            }));*/

            List<? extends ImportAdapterFactoryConfig> configs = mapper.convertValue(decodedConfig,
                    mapper.getTypeFactory().constructCollectionType (ArrayList.class, DefaultImportAdapterFactoryConfig.class));
            //new TypeReference<List<? extends ImportAdapterFactoryConfig>>() {}
            return configs;
        }
        catch (Throwable t){
            log.error("Error fetching import factory config");
            throw t;
        }

    }
}
