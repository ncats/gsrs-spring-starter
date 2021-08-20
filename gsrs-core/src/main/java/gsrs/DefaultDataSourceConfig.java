package gsrs;

import org.springframework.beans.factory.annotation.Qualifier;
        import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
        import org.springframework.boot.context.properties.ConfigurationProperties;
        import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.context.annotation.Primary;
        import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
        import org.springframework.orm.jpa.JpaTransactionManager;
        import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

        import javax.persistence.EntityManagerFactory;
        import javax.sql.DataSource;
        import java.util.HashMap;
        import java.util.Map;

// first link used in approach below.
// https://stackoverflow.com/questions/45663025/spring-data-jpa-multiple-enablejparepositories
// https://www.baeldung.com/spring-data-jpa-multiple-databases
// https://springframework.guru/how-to-configure-multiple-data-sources-in-a-spring-boot-application/
// https://www.kindsonthegenius.com/microservices/multiple-database-configuration-for-microservice-in-spring-boot/
// https://www.javadevjournal.com/spring-boot/multiple-data-sources-with-spring-boot/

@Configuration
@EnableJpaRepositories(
        entityManagerFactoryRef = "defaultEntityManager",
        transactionManagerRef = "defaultTransactionManager",
        basePackages = {"ix","gsrs", "gov.nih.ncats"}
)
public class DefaultDataSourceConfig {
    static {
        System.out.println("!!!!!!!");
    }

    @Bean(name = "defaultEntityManager")
    @Primary
    public LocalContainerEntityManagerFactoryBean getDefaultEntityManager(EntityManagerFactoryBuilder builder,
                                                                          @Qualifier("defaultDataSource") DataSource defaultDataSource){


        return builder
                .dataSource(defaultDataSource)
                .packages("ix","gsrs", "gov.nih.ncats")
                .persistenceUnit("default")
                .properties(additionalJpaProperties())
                .build();

    }

    
    //TODO: I don't think this is necessary?
    Map<String,?> additionalJpaProperties(){
        System.out.println("GETTING");
        Map<String,String> map = new HashMap<>();

        map.put("hibernate.hbm2ddl.auto", "create");
        map.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect"); //this is probably dangerous
        
//        map.put("hibernate.show_sql", "true"); //this too?

        return map;
    }


    @Bean("defaultDataSourceProperties")
    @Primary
    @ConfigurationProperties(prefix = "default.datasource")
    public DataSourceProperties defaultDataSourceProperties(){
        return new DataSourceProperties();
    }



    @Bean("defaultDataSource")
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource defaultDataSource(@Qualifier("defaultDataSourceProperties") DataSourceProperties defaultDataSourceProperties) {
        return defaultDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean(name = "defaultTransactionManager")
    @Primary
    public JpaTransactionManager transactionManager(@Qualifier("defaultEntityManager") EntityManagerFactory defaultEntityManager){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(defaultEntityManager);

        return transactionManager;
    }
}