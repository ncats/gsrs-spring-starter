package gsrs;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import lombok.extern.slf4j.Slf4j;

// first link used in approach below.
// https://stackoverflow.com/questions/45663025/spring-data-jpa-multiple-enablejparepositories
// https://www.baeldung.com/spring-data-jpa-multiple-databases
// https://springframework.guru/how-to-configure-multiple-data-sources-in-a-spring-boot-application/
// https://www.kindsonthegenius.com/microservices/multiple-database-configuration-for-microservice-in-spring-boot/
// https://www.javadevjournal.com/spring-boot/multiple-data-sources-with-spring-boot/

@Configuration
@EnableJpaRepositories(
        entityManagerFactoryRef = DefaultDataSourceConfig.NAME_ENTITY_MANAGER,
        transactionManagerRef = DefaultDataSourceConfig.NAME_TRANSACTION_MANAGER,
        basePackages = {"ix", "gsrs", "gov.nih.ncats"}
)
@Slf4j
public class DefaultDataSourceConfig {
    //These 3 things and the basePackages above are the typical things that
    //may need to change if trying to make a new DataSourceConfig
    protected static final String[] BASE_PACKAGES = new String[] {"ix", "gsrs", "gov.nih.ncats"};    
    protected static final String PERSIST_UNIT = "default";
    protected static final String DATASOURCE_PROPERTY_PATH_PREFIX = "spring"; 
    
    //In most other cases you will want this variable to be the same as the PERSIST_UNIT
    //as shown below:
//    protected static final String DATASOURCE_PROPERTY_PATH_PREFIX = PERSIST_UNIT; 
    
    
    // As-written, this shouldn't have to change when adapting
    // but there may be cases where it is desirable to change
    protected static final String DATASOURCE_PROPERTY_PATH_FULL = DATASOURCE_PROPERTY_PATH_PREFIX + ".datasource";
    
    //These below shouldn't have to change and are built from the constants above
    protected static final String NAME_DATA_SOURCE = PERSIST_UNIT + "DataSource";
    public static final String NAME_ENTITY_MANAGER = PERSIST_UNIT + "EntityManager";
    protected static final String NAME_DATA_SOURCE_PROPERTIES = PERSIST_UNIT + "DataSourceProperties";
    protected static final String NAME_TRANSACTION_MANAGER = PERSIST_UNIT + "TransactionManager";
    
       
    @Autowired
    private Environment env;
    

    @Bean(name = NAME_ENTITY_MANAGER)
    @Primary
    public LocalContainerEntityManagerFactoryBean getDefaultEntityManager(EntityManagerFactoryBuilder builder,
                                                                          @Qualifier(NAME_DATA_SOURCE) DataSource defaultDataSource){


        return builder
                .dataSource(defaultDataSource)
                .packages(BASE_PACKAGES)
                .persistenceUnit(PERSIST_UNIT)
                .properties(additionalJpaProperties())
                .build();

    }

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
    
    
    // TODO: This needs to be thought about for what properties are needed
    // beyond the few specified here.
    private Map<String,?> additionalJpaProperties(){
        
        //For each of these the logic should really be to look:
        // 1. For the specific case. If it's present and the word "null", consider the property null. If it's present and anything else, use the property.
        // 2. If there is no specific property, look for the more generic one, use it if not null
        
        
        Map<String,String> map = new HashMap<>();
//spring.jpa.hibernate.use-new-id-generator-mappings
        
        Optional<String> dialect = getProperty(DATASOURCE_PROPERTY_PATH_PREFIX + ".jpa.database-platform", "spring.jpa.database-platform");
        Optional<String> ddlSetting = getProperty(DATASOURCE_PROPERTY_PATH_PREFIX + ".jpa.hibernate.ddl-auto", "spring.jpa.hibernate.ddl-auto", "update");
        Optional<String> showSQL = getProperty(DATASOURCE_PROPERTY_PATH_PREFIX + ".hibernate.show_sql", "hibernate.show_sql");
        Optional<String> newIDGen = getProperty(DATASOURCE_PROPERTY_PATH_PREFIX + ".jpa.hibernate.use-new-id-generator-mappings", "spring.jpa.hibernate.use-new-id-generator-mappings", "true");
                

        log.debug("dialect:" + dialect.orElse(null));
        log.debug("Show SQL:" + showSQL.orElse(null));
        log.debug("DDL:" + ddlSetting.orElse(null));
        log.debug("use-new-id-generator-mappings:" + newIDGen.orElse(null));
        
        ddlSetting.ifPresent(d->map.put("hibernate.hbm2ddl.auto", d));
        showSQL.ifPresent(d->map.put("hibernate.show_sql", d));
        dialect.ifPresent(d->map.put("hibernate.dialect", d));
        //need to test
        newIDGen.ifPresent(d->map.put("hibernate.use-new-id-generator-mappings", d));
        
        return map;
    }


    // TP 08-20-2021 By setting this to be "spring.datasource"
    // it honors the default syntax
    @Bean(NAME_DATA_SOURCE_PROPERTIES)
    @Primary
    @ConfigurationProperties(DATASOURCE_PROPERTY_PATH_FULL)
    public DataSourceProperties defaultDataSourceProperties(){
        return new DataSourceProperties();
    }



    @Bean(NAME_DATA_SOURCE)
    @Primary
    @ConfigurationProperties(DATASOURCE_PROPERTY_PATH_FULL)
    public DataSource defaultDataSource(@Qualifier(NAME_DATA_SOURCE_PROPERTIES) DataSourceProperties defaultDataSourceProperties) {
        return defaultDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean(name = NAME_TRANSACTION_MANAGER)
    @Primary
    public JpaTransactionManager transactionManager(@Qualifier(NAME_ENTITY_MANAGER) EntityManagerFactory defaultEntityManager){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(defaultEntityManager);
        return transactionManager;
    }
}
