package gsrs;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import ix.core.EbeanLikeImplicitNamingStategy;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public abstract class GSRSDataSourceConfig {

    @Autowired
    private Environment env;
    

    private Optional<String> getProperty(String key1, String key2){
        return getProperty(key1,key2, null);
    }
    private Optional<String> getProperty(String key1, String key2, String def){
        String prop1 = env.getProperty(key1);
        if(prop1!=null ) {
            if(prop1.equals("null")) {
                return Optional.ofNullable(def);    
            }
            return Optional.of(prop1);
        }else {
            String prop2 = env.getProperty(key2);
            if(prop2!=null ) {
                if(prop2.equals("null")) {
                    return Optional.ofNullable(def);    
                }
                return Optional.of(prop2);
            }
            return Optional.ofNullable(def);
        }
        
    }
    public Map<String,?> additionalJpaProperties(String DATASOURCE_PROPERTY_PATH_PREFIX){

        //For each of these the logic should really be to look:
        // 1. For the specific case. If it's present and the word "null", consider the property null. If it's present and anything else, use the property.
        // 2. If there is no specific property, look for the more generic one, use it if not null


        Map<String,String> map = new HashMap<>();
        //spring.jpa.hibernate.use-new-id-generator-mappings

        Optional<String> dialect = getProperty(DATASOURCE_PROPERTY_PATH_PREFIX + ".jpa.database-platform", "spring.jpa.database-platform");
        Optional<String> ddlSetting = getProperty(DATASOURCE_PROPERTY_PATH_PREFIX + ".jpa.hibernate.ddl-auto", "spring.jpa.hibernate.ddl-auto", "update");
        Optional<String> showSQL = getProperty(DATASOURCE_PROPERTY_PATH_PREFIX + ".hibernate.show_sql", "hibernate.show_sql");
        Optional<String> newIDGen = getProperty(DATASOURCE_PROPERTY_PATH_PREFIX + ".jpa.hibernate.use-new-id-generator-mappings", "spring.jpa.hibernate.use-new-id-generator-mappings", "true");
        Optional<String> dirtiness = getProperty(DATASOURCE_PROPERTY_PATH_PREFIX + ".jpa.properties.hibernate.entity_dirtiness_strategy", "spring.jpa.properties.hibernate.entity_dirtiness_strategy", "gsrs.GsrsEntityDirtinessStrategy");
        Optional<String> formatSQL = getProperty(DATASOURCE_PROPERTY_PATH_PREFIX + ".jpa.properties.hibernate.format_sql", "hibernate.format_sql");

        log.debug("dialect:" + dialect.orElse(null));
        log.debug("Show SQL:" + showSQL.orElse(null));
        log.debug("DDL:" + ddlSetting.orElse(null));
        log.debug("use-new-id-generator-mappings:" + newIDGen.orElse(null));
        
        log.debug("dirtiness Strat:" + dirtiness.orElse(null));

        ddlSetting.ifPresent(d->map.put("hibernate.hbm2ddl.auto", d));
        showSQL.ifPresent(d->map.put("hibernate.show_sql", d));
        dialect.ifPresent(d->map.put("hibernate.dialect", d));
        //need to test
        newIDGen.ifPresent(d->map.put("hibernate.use-new-id-generator-mappings", d));
        newIDGen.ifPresent(d->map.put("hibernate.id.new_generator_mappings", d));
        formatSQL.ifPresent(d->map.put("hibernate.format_sql", d));

        dirtiness.ifPresent(d->map.put("hibernate.entity_dirtiness_strategy", d));

        //This doesn't seem ideal ... but it may be the only way
        map.put("hibernate.physical_naming_strategy", PhysicalNamingStrategyStandardImpl.class.getName());
        map.put("hibernate.implicit_naming_strategy", EbeanLikeImplicitNamingStategy.class.getName());
        
                
                
                

        return map;
    }
}
