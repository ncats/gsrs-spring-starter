package gsrs;

import akka.event.Logging;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.entityProcessor.EntityProcessorConfig;
import gsrs.imports.ImportAdapterFactoryConfig;
import gsrs.validator.ValidatorConfig;
import gsrs.validator.ValidatorConfigList;
import ix.core.util.EntityUtils;
import lombok.Data;
import lombok.SneakyThrows;
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

    @Value("${gsrs.raw.importAdapterFactories.substances}")
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
        //using 'warn' because 'trace' does not work
        log.warn("starting in getImportAdapterFactories.  context: " + context);
        if(importAdapterFactories ==null) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> list = importAdapterFactories.get(context);
            log.trace("list:");
            list.forEach(i->i.keySet().forEach(k->log.warn(String.format("key: %s; value: %s", k, i.get(k)))));

            if(list==null || list.isEmpty()){
                log.warn("no import adapter factory configuration info found!");
                return Collections.emptyList();
            }
            List<? extends ImportAdapterFactoryConfig> configs = EntityUtils.convertClean(list, new TypeReference<List<? extends ImportAdapterFactoryConfig>>() {});
            log.warn("total configs: " + configs.size());
            return configs;
        }
        catch (Throwable t){
            log.error("Error fetching import factory config");
            throw t;
        }
    }
    /* tested this but using Tyler's implementation, above
    @SneakyThrows
    public List<? extends ImportAdapterFactoryConfig> getImportAdapterFactories(String context) {
        log.warn("starting in getImportAdapterFactories. context: " +context);
        log.warn("rawImportConfigString: " + rawImportConfigString);
        String decodedConfig="";
        if( rawImportConfigString!= null && rawImportConfigString.trim().length() >0) {
            try {
                decodedConfig = URLDecoder.decode(rawImportConfigString, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error("error decoding data " + e.getMessage());
                e.printStackTrace();
            }
        }
        if(importAdapterFactories ==null && (decodedConfig == null || decodedConfig.length()==0)) {
            log.warn("going to return empty list");
            return Collections.emptyList();
        }
        ObjectMapper mapper = new ObjectMapper();

        try {
            log.warn("decodedConfig: " + decodedConfig);
            List<? extends ImportAdapterFactoryConfig> configs = mapper.readValue(decodedConfig,
                    new TypeReference<List<? extends ImportAdapterFactoryConfig>>() {});
//                    mapper.getTypeFactory().constructCollectionType (ArrayList.class, DefaultImportAdapterFactoryConfig.class));
            //new TypeReference<List<? extends ImportAdapterFactoryConfig>>() {}
            return configs;
        }
        catch (Exception t){
            log.error("Error fetching import factory config");
            throw t;
        }

    }*/
}
